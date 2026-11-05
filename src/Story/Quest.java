package Story;

import java.util.ArrayList;
import java.util.List;

// STORY: one quest as the quest log shows it. The objectives are the quest's stages in order (stage 0 = the first);
// "%n" in an objective is replaced by its kill counter (see Quests.progress). What actually moves a quest forward lives in
// StoryDirector; this is only the text.
public final class Quest {
    public final String id, title, giver, place, summary, reward;
    public final int world;
    public final boolean main;
    public final int faction;          // StoryState.CONCORD / CHORUS / KEPT, or -1 for none
    public final String[] objectives;

    Quest(String id, int world, boolean main, int faction, String title, String giver, String place, String summary,
          String reward, String... objectives) {
        this.id = id;
        this.world = world;
        this.main = main;
        this.faction = faction;
        this.title = title;
        this.giver = giver;
        this.place = place;
        this.summary = summary;
        this.reward = reward;
        this.objectives = objectives;
    }

    // ---------------------------------------------------------------- World 1
    // Main quests run in order (each one starts when the one before it ends); side quests can be picked up at any time.
    private static final List<Quest> ALL = new ArrayList<>();

    static {
        // main
        add(new Quest("w1_ration", 1, true, -1, "THE LAST RATION", "WREN", "BRACKENFORD",
                "A girl in the square asked you to carry her family's ration to the widow Hester. Bread is worth more than coin now.",
                "RUSTED SHORTSWORD, 25 GOLD",
                "TAKE THE RATION TO THE WIDOW HESTER IN BRACKENFORD.",
                "RETURN TO WREN IN THE SQUARE."));
        add(new Quest("w1_salt", 1, true, -1, "SALT IN THE FURROWS", "IDA", "BARREN FIELDS",
                "Ida's husband Tomas went to the Barren Fields to dig for roots three days ago and never came back.",
                "WARPED BUCKLER, 40 GOLD",
                "SEARCH THE BARREN FIELDS SOUTH OF TOWN FOR TOMAS.",
                "DEFEAT STARVING WRETCHES IN THE BARREN FIELDS (%n/3).",
                "TELL IDA WHAT YOU FOUND."));
        add(new Quest("w1_seed", 1, true, CHORUSQ(), "SEED FOR THE LAST WINTER", "IDA", "GRANARY CLOISTER",
                "Wren has a fever and the town has no grain. The Hollow Chorus keeps seed grain in the cellars of the Granary Cloister.",
                "60 GOLD",
                "ASK THE HOLLOW CHORUS AT THE GRANARY CLOISTER FOR GRAIN.",
                "WAIT THROUGH THE CHORUS'S RITE, THEN SPEAK TO BROTHER ANSEL.",
                "BRING THE GRAIN TO WREN IN BRACKENFORD."));
        add(new Quest("w1_water", 1, true, -1, "WHERE THE WATER WENT", "HESTER", "DRY RIVER",
                "Wren drew the river in the dirt before she died. She said it did not dry up. It went down, into the ground.",
                "CRACKED HUNTING BOW, 50 GOLD",
                "FOLLOW THE DRY RIVER SOUTH-EAST OF TOWN.",
                "FIND WHAT CRACKED THE RIVERBED."));
        add(new Quest("w1_graves", 1, true, -1, "GRAVES THAT WALK", "THE MARK", "BARROWS",
                "Great claw marks lead away from the river, north, toward the old burial mounds where the Reckoners camp.",
                "SCAVENGER'S HIDE, 70 GOLD",
                "GO NORTH TO THE BARROWS.",
                "DRIVE OFF THE RECKONERS AT THE BARROWS (%n/3).",
                "SPEAK TO THE WOUNDED RECKONER.",
                "SEARCH THE OLDEST BURIAL MOUND."));
        add(new Quest("w1_warden", 1, true, -1, "THE FAMINE WARDEN", "HALE", "ASHWOOD",
                "The Reckoners serve a warden who takes the last of every harvest in Dabbah's name. He keeps his tithe in Ashwood.",
                "THE TABLET'S KEYSTONE, 120 GOLD",
                "FIND THE FAMINE WARDEN IN ASHWOOD, NORTH-WEST.",
                "FACE THE FAMINE WARDEN."));
        add(new Quest("w1_kept", 1, true, KEPTQ(), "THE KEPT REMEMBER", "OLD MAUD", "KEPT GROVE",
                "The Warden's carving speaks of a compact older than the walls. The Kept Grove, west of Brackenford, still remembers it.",
                "THE WAY TO THE PAST",
                "FIND THE KEPT AT KEPT GROVE, WEST OF BRACKENFORD.",
                "STEP THROUGH THE STANDING STONE WEST OF TOWN."));
        // side
        add(new Quest("s1_recruit", 1, false, CONCORDQ(), "RECRUITING THE HUNGRY", "CAPTAIN VALE", "BRACKENFORD",
                "A Concord captain offers a loaf of bread for every boy who signs on to march against the Beast.",
                "DEPENDS ON YOUR CHOICE",
                "DECIDE WHAT TO DO ABOUT CAPTAIN VALE'S OFFER.",
                "WARN THE MOTHERS: TALK TO BESS IN THE SQUARE."));
        add(new Quest("s1_tithe", 1, false, CHORUSQ(), "THE TITHE BASKET", "SISTER MIREL", "GRANARY CLOISTER",
                "The Chorus collects a tithe of bread for the rite. Someone has to carry the basket to Brother Ansel.",
                "DEPENDS ON YOUR CHOICE",
                "CARRY THE TITHE BASKET TO BROTHER ANSEL."));
        add(new Quest("s1_bury", 1, false, KEPTQ(), "BURY THE WRETCHES", "ELSA", "BARREN FIELDS",
                "The Starving Wretches were people once. Elsa of the Kept wants something of each brought back to bury.",
                "60 GOLD",
                "TAKE TOKENS FROM STARVING WRETCHES YOU DEFEAT (%n/5).",
                "BRING THE TOKENS TO ELSA IN THE BARREN FIELDS."));
        add(new Quest("s1_names", 1, false, -1, "NAMES ON THE WALL", "THE WALL", "BRACKENFORD",
                "A wall in the square is covered in scratched names: the famine dead. Each one has a story.",
                "A TABLET PIECE",
                "READ THE NAMES ON THE WALL (%n/6)."));
    }

    private static int CONCORDQ() { return StoryState.CONCORD; }
    private static int CHORUSQ() { return StoryState.CHORUS; }
    private static int KEPTQ() { return StoryState.KEPT; }

    private static void add(Quest q) { ALL.add(q); }

    public static List<Quest> all() { return ALL; }

    public static Quest get(String id) {
        for (Quest q : ALL) if (q.id.equals(id)) return q;
        return null;
    }

    // ---------------------------------------------------------------- directions (main quests only)
    // where the story goes after a main quest is finished: shown as "NEXT: ..." when it completes, and in the journal
    public static String nextAfter(String id) {
        switch (id) {
            case "w1_ration": return "FIND IDA BY THE OLD CART, ON THE EAST SIDE OF THE BRACKENFORD SQUARE.";
            case "w1_salt": return "THE GRANARY CLOISTER. FOLLOW THE ROAD EAST OUT OF BRACKENFORD.";
            case "w1_seed": return "THE DRY RIVER, SOUTH-EAST OF BRACKENFORD, PAST THE BARREN FIELDS.";
            case "w1_water": return "THE BARROWS, FAR TO THE NORTH-EAST, UP THE VALLEY.";
            case "w1_graves": return "ASHWOOD, FAR TO THE NORTH-WEST, ACROSS THE VALLEY.";
            case "w1_warden": return "OLD MAUD AT KEPT GROVE, WEST OF BRACKENFORD ALONG THE WEST ROAD.";
            case "w1_kept": return "WORLD 2: THE PAST. ELDEREMERE.";
            default: return null;
        }
    }

    // where the current step of a main quest takes place (the journal's WHERE line); null for side quests
    public static String whereNow(Quest q) {
        if (!q.main) return null;
        int s = StoryState.stage(q.id);
        switch (q.id) {
            case "w1_ration": return "BRACKENFORD SQUARE (WHERE YOU START)";
            case "w1_salt": return s >= 2 ? "BRACKENFORD SQUARE: IDA" : "THE BARREN FIELDS, SOUTH OF BRACKENFORD";
            case "w1_seed": return s >= 2 ? "BRACKENFORD, BY THE DRY WELL" : "THE GRANARY CLOISTER, EAST OF BRACKENFORD";
            case "w1_water": return "THE DRY RIVER, SOUTH-EAST OF BRACKENFORD";
            case "w1_graves": return "THE BARROWS, NORTH-EAST";
            case "w1_warden": return "ASHWOOD, NORTH-WEST";
            case "w1_kept": return s >= 1 ? "THE STANDING STONE, WEST OF BRACKENFORD" : "KEPT GROVE, WEST OF BRACKENFORD";
            default: return null;
        }
    }

    // the counter shown in an objective's "%n"
    public static String progress(Quest q, String objective) {
        if (!objective.contains("%n")) return objective;
        String counter = q.id.equals("w1_salt") ? "w1_salt_kills" : q.id.equals("w1_graves") ? "w1_graves_kills"
                : q.id.equals("s1_bury") ? "s1_bury_tokens" : "s1_names_read";
        return objective.replace("%n", String.valueOf(StoryState.count(counter)));
    }
}