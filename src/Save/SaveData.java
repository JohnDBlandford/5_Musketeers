package Save;

import java.util.ArrayList;
import java.util.List;

public class SaveData {

    public static final int VERSION = 1;

    // where the player is
    public String mapName = "world1";
    public float playerX;
    public float playerY;
    public String facingDirection = "LEFT";

    // player progression
    public int level = 1;
    public int currentXP = 0;
    public int xpToNextLevel = 100;
    public float maxHP = 100.0f;
    public float currentHP = 100.0f;
    public float baseATK = 15.0f;
    public float baseDEF = 10.0f;
    public float baseSPD = 25.0f;
    public int gold = 0;

    public List<String> discoveredPlaces = new ArrayList<>();
    public String lastPlace;

    public List<ItemSave> items = new ArrayList<>();

    // STORY: quests, choices, faction standing and tablet pieces, as one line (Story.StoryState.serialize). Empty in saves
    // made before the story existed; those load as a fresh story.
    public String story = "";

    public static class ItemSave {
        public String className;
        public int level;
        public long xp;
        public int durability;
        public boolean equipped;

        public ItemSave(String className, int level, long xp, int durability, boolean equipped) {
            this.className = className;
            this.level = level;
            this.xp = xp;
            this.durability = durability;
            this.equipped = equipped;
        }
    }
}