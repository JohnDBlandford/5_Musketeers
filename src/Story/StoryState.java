package Story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// STORY: everything the story remembers for the whole game. It lives outside the maps (they are rebuilt on every map change)
// so a quest, a choice or a tablet piece survives portals, doors, battles and loading a save.
//
//   quest stages      stage(id): -1 = not started, 0.. = the objective being worked on, DONE = finished
//   flags             every choice the knight made ("w1_ration_whole", "w1_vale_warned", ...)
//   standing          Concord / Hollow Chorus / Kept Grove, each from -5 to +5 (the quest log shows them as bars)
//   counters          kill counts for quests ("w1_salt_kills")
//   tablet pieces     pieces[world][0..5]; piece 5 is the KEYSTONE, which only the world's boss drops
//   choice log        one line per choice, shown in the quest log so the player sees what they decided
//
// serialize() / load() turn it into one line of text for the save file (SaveData.story).
public final class StoryState {
    private StoryState() {}

    public static final int DONE = 100;
    public static final int CONCORD = 0, CHORUS = 1, KEPT = 2;
    public static final String[] FACTIONS = { "CONCORD", "HOLLOW CHORUS", "KEPT GROVE" };
    public static final int PIECES = 6, KEYSTONE = 5;

    private static final Map<String, Integer> stages = new HashMap<>();
    private static final Set<String> flags = new LinkedHashSet<>();
    private static final Map<String, Integer> counters = new HashMap<>();
    private static final int[] standing = new int[3];
    private static final boolean[][] pieces = new boolean[4][PIECES];
    private static final List<String> choiceLog = new ArrayList<>();

    // ---------------------------------------------------------------- a new game
    public static void reset() {
        stages.clear();
        flags.clear();
        counters.clear();
        java.util.Arrays.fill(standing, 0);
        for (boolean[] w : pieces) java.util.Arrays.fill(w, false);
        choiceLog.clear();
    }

    // ---------------------------------------------------------------- quests
    public static int stage(String quest) { return stages.getOrDefault(quest, -1); }
    public static boolean started(String quest) { return stage(quest) >= 0; }
    public static boolean done(String quest) { return stage(quest) >= DONE; }
    public static boolean active(String quest) { int s = stage(quest); return s >= 0 && s < DONE; }
    public static boolean at(String quest, int s) { return stage(quest) == s; }
    public static void setStage(String quest, int s) { stages.put(quest, s); }

    // ---------------------------------------------------------------- flags and counters
    public static boolean flag(String f) { return flags.contains(f); }
    public static void set(String f) { flags.add(f); }
    public static int count(String c) { return counters.getOrDefault(c, 0); }
    public static int add(String c, int n) { int v = count(c) + n; counters.put(c, v); return v; }

    // ---------------------------------------------------------------- standing
    public static int standing(int faction) { return standing[faction]; }

    public static void shift(int faction, int delta) {
        standing[faction] = Math.max(-5, Math.min(5, standing[faction] + delta));
    }

    // the word the quest log shows for a standing
    public static String standingWord(int v) {
        if (v <= -4) return "HOSTILE";
        if (v <= -2) return "DISTRUSTED";
        if (v <= 1) return "NEUTRAL";
        if (v <= 3) return "TRUSTED";
        return "SWORN";
    }

    // ---------------------------------------------------------------- tablet pieces
    public static boolean hasPiece(int world, int piece) { return pieces[world][piece]; }

    // returns true if this call gave the piece (false if the knight already had it)
    public static boolean givePiece(int world, int piece) {
        if (pieces[world][piece]) return false;
        pieces[world][piece] = true;
        return true;
    }

    public static int pieceCount(int world) {
        int n = 0;
        for (boolean b : pieces[world]) if (b) n++;
        return n;
    }

    public static boolean tabletComplete(int world) { return pieceCount(world) == PIECES; }

    // ---------------------------------------------------------------- the choice log
    public static void logChoice(String line) { choiceLog.add(line); }
    public static List<String> choices() { return choiceLog; }

    // ---------------------------------------------------------------- save / load (one line of text)
    // format: section|section|...   q:id=stage,id=stage  f:a,b  c:id=n  s:c,h,k  p:world=bits  l:line~line
    public static String serialize() {
        StringBuilder b = new StringBuilder("v1");
        b.append("|q:");
        for (Map.Entry<String, Integer> e : stages.entrySet()) b.append(e.getKey()).append('=').append(e.getValue()).append(',');
        b.append("|f:").append(String.join(",", flags));
        b.append("|c:");
        for (Map.Entry<String, Integer> e : counters.entrySet()) b.append(e.getKey()).append('=').append(e.getValue()).append(',');
        b.append("|s:").append(standing[0]).append(',').append(standing[1]).append(',').append(standing[2]);
        b.append("|p:");
        for (int w = 1; w < pieces.length; w++) {
            int bits = 0;
            for (int i = 0; i < PIECES; i++) if (pieces[w][i]) bits |= 1 << i;
            b.append(w).append('=').append(bits).append(',');
        }
        b.append("|l:");
        for (int i = 0; i < choiceLog.size(); i++) {
            if (i > 0) b.append('~');
            b.append(choiceLog.get(i).replace("|", "/").replace("~", "-"));
        }
        return b.toString();
    }

    // an empty or unreadable string starts a fresh story (old saves made before the story existed load fine)
    public static void load(String data) {
        reset();
        if (data == null || !data.startsWith("v1")) return;
        try {
            for (String section : data.split("\\|")) {
                if (section.length() < 2 || section.charAt(1) != ':') continue;
                String body = section.substring(2);
                switch (section.charAt(0)) {
                    case 'q':
                        for (String kv : body.split(",")) if (kv.contains("=")) {
                            String[] p = kv.split("=");
                            stages.put(p[0], Integer.parseInt(p[1]));
                        }
                        break;
                    case 'f':
                        for (String f : body.split(",")) if (!f.isEmpty()) flags.add(f);
                        break;
                    case 'c':
                        for (String kv : body.split(",")) if (kv.contains("=")) {
                            String[] p = kv.split("=");
                            counters.put(p[0], Integer.parseInt(p[1]));
                        }
                        break;
                    case 's': {
                        String[] p = body.split(",");
                        for (int i = 0; i < 3 && i < p.length; i++) standing[i] = Integer.parseInt(p[i].trim());
                        break;
                    }
                    case 'p':
                        for (String kv : body.split(",")) if (kv.contains("=")) {
                            String[] p = kv.split("=");
                            int w = Integer.parseInt(p[0]), bits = Integer.parseInt(p[1]);
                            if (w > 0 && w < pieces.length)
                                for (int i = 0; i < PIECES; i++) pieces[w][i] = (bits & (1 << i)) != 0;
                        }
                        break;
                    case 'l':
                        for (String line : body.split("~")) if (!line.isEmpty()) choiceLog.add(line);
                        break;
                    default: break;
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Story: could not read the saved story (" + e + "), starting fresh");
            reset();
        }
    }
}