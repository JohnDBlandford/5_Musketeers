package Save;

import Engine.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Save {
    public static final int DEFAULT_SLOT = 1;

    private Save() {
    }

    public static boolean exists(int slot) {
        return fileFor(slot).isFile();
    }

    public static boolean delete(int slot) {
        return fileFor(slot).delete();
    }

    // returns true if the save was written
    public static boolean save(int slot, SaveData data) {
        File file = fileFor(slot);
        File temp = new File(file.getPath() + ".tmp");
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8)) {
                toProperties(data).store(writer, "5 Musketeers save file");
            }
            if (file.exists() && !file.delete())
                throw new IOException("could not replace " + file);
            if (!temp.renameTo(file))
                throw new IOException("could not move " + temp + " to " + file);
            return true;
        } catch (IOException e) {
            System.err.println("Save failed for slot " + slot + ": " + e.getMessage());
            temp.delete();
            return false;
        }
    }

    // returns null if the slot is empty or the file can't be read
    public static SaveData load(int slot) {
        return load(fileFor(slot));
    }

    // a save file found on disk: its display name and when it was last written
    public static final class SaveInfo {
        public final String name;
        public final File file;
        public final long savedAt; // millis since epoch

        SaveInfo(String name, File file, long savedAt) {
            this.name = name;
            this.file = file;
            this.savedAt = savedAt;
        }
    }

    // every readable save in the save folder, newest first
    public static List<SaveInfo> listSaves() {
        List<SaveInfo> saves = new ArrayList<>();
        File[] files = new File(Config.SAVE_FILES_PATH).listFiles((dir, n) -> n.endsWith(".txt"));
        if (files == null)
            return saves;
        for (File f : files) {
            if (!f.isFile() || f.length() == 0 || load(f) == null)
                continue; // skip empty / placeholder / broken files
            String name = f.getName().substring(0, f.getName().length() - 4);
            saves.add(new SaveInfo(name, f, f.lastModified()));
        }
        saves.sort((a, b) -> Long.compare(b.savedAt, a.savedAt));
        return saves;
    }

    // returns null if the file is missing or can't be read
    public static SaveData load(File file) {
        if (!file.isFile())
            return null;
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
            return fromProperties(props);
        } catch (IOException | RuntimeException e) {
            System.err.println("Load failed for " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static File fileFor(int slot) {
        return new File(Config.SAVE_FILES_PATH + "save" + ".txt");
    }

    private static Properties toProperties(SaveData data) {
        Properties p = new Properties();
        p.setProperty("version", String.valueOf(SaveData.VERSION));

        p.setProperty("map", data.mapName);
        p.setProperty("player.x", String.valueOf(data.playerX));
        p.setProperty("player.y", String.valueOf(data.playerY));
        p.setProperty("player.facing", data.facingDirection);
        p.setProperty("player.level", String.valueOf(data.level));
        p.setProperty("player.xp", String.valueOf(data.currentXP));
        p.setProperty("player.xpToNextLevel", String.valueOf(data.xpToNextLevel));
        p.setProperty("player.maxHP", String.valueOf(data.maxHP));
        p.setProperty("player.currentHP", String.valueOf(data.currentHP));
        p.setProperty("player.atk", String.valueOf(data.baseATK));
        p.setProperty("player.def", String.valueOf(data.baseDEF));
        p.setProperty("player.spd", String.valueOf(data.baseSPD));
        p.setProperty("gold", String.valueOf(data.gold));

        p.setProperty("places.count", String.valueOf(data.discoveredPlaces.size()));
        for (int i = 0; i < data.discoveredPlaces.size(); i++)
            p.setProperty("places." + i, data.discoveredPlaces.get(i));
        if (data.lastPlace != null)
            p.setProperty("places.last", data.lastPlace);

        p.setProperty("items.count", String.valueOf(data.items.size()));
        for (int i = 0; i < data.items.size(); i++) {
            SaveData.ItemSave item = data.items.get(i);
            String key = "items." + i + ".";
            p.setProperty(key + "class", item.className);
            p.setProperty(key + "level", String.valueOf(item.level));
            p.setProperty(key + "xp", String.valueOf(item.xp));
            p.setProperty(key + "durability", String.valueOf(item.durability));
            p.setProperty(key + "equipped", String.valueOf(item.equipped));
        }
        return p;
    }

    private static SaveData fromProperties(Properties p) {
        int version = Integer.parseInt(required(p, "version"));
        if (version != SaveData.VERSION) {
            throw new IllegalArgumentException(
                    "save version " + version + " is not supported (expected " + SaveData.VERSION + ")");
        }
        SaveData data = new SaveData();

        data.mapName = required(p, "map");
        data.playerX = Float.parseFloat(required(p, "player.x"));
        data.playerY = Float.parseFloat(required(p, "player.y"));
        data.facingDirection = p.getProperty("player.facing", data.facingDirection);
        data.level = Integer.parseInt(required(p, "player.level"));
        data.currentXP = Integer.parseInt(required(p, "player.xp"));
        data.xpToNextLevel = Integer.parseInt(required(p, "player.xpToNextLevel"));
        data.maxHP = Float.parseFloat(required(p, "player.maxHP"));
        data.currentHP = Float.parseFloat(required(p, "player.currentHP"));
        data.baseATK = Float.parseFloat(required(p, "player.atk"));
        data.baseDEF = Float.parseFloat(required(p, "player.def"));
        data.baseSPD = Float.parseFloat(required(p, "player.spd"));
        data.gold = Integer.parseInt(required(p, "gold"));

        int placeCount = Integer.parseInt(p.getProperty("places.count", "0"));
        for (int i = 0; i < placeCount; i++)
            data.discoveredPlaces.add(required(p, "places." + i));
        data.lastPlace = p.getProperty("places.last");

        int itemCount = Integer.parseInt(p.getProperty("items.count", "0"));
        for (int i = 0; i < itemCount; i++) {
            String key = "items." + i + ".";
            data.items.add(new SaveData.ItemSave(
                    required(p, key + "class"),
                    Integer.parseInt(required(p, key + "level")),
                    Long.parseLong(required(p, key + "xp")),
                    Integer.parseInt(required(p, key + "durability")),
                    Boolean.parseBoolean(p.getProperty(key + "equipped", "false"))));
        }
        return data;
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null)
            throw new IllegalArgumentException("missing \"" + key + "\"");
        return value;
    }
}
