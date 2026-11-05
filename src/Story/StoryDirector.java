package Story;

import BattleSystem.Balance;
import BattleSystem.BattleUI;
import Encounters.EnemyFactory;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.ScreenManager;
import Item.Item;
import Level.Player;
import NPCs.Enemy;
import Story.NPCArt.Look;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// STORY: World 1's story. Every conversation, every choice and what it changes, every quest step, the tablet pieces, and
// the little notices at the top of the screen ("NEW QUEST", "+1 KEPT GROVE", "TABLET PIECE 3/6").
//
// PlayLevelScreen owns one and calls it:
//   updateOverlays(canOpenLog)   every frame first; true = a conversation / the quest log (Q) / a tablet is open, so the
//                                world stays frozen
//   openStoryDialogue(id)        a story character was talked to
//   onPlaceEntered(name)         the knight walked into a named place
//   onVictory(enemy, place)      a battle was won (kill counts)
//   onStoryBattleEnd(tag, won)   a fight the story started has ended
//   drawToasts / drawOverlays    after the map is drawn
//
// The World 1 quests (see Quest.java and the Quest & Travel Plan doc): 7 main quests in order, 4 side quests, the World 1
// tablet "The Sentence" in 6 pieces (piece 5, the keystone, is the Famine Warden's).
public final class StoryDirector {
    // the director of the running game (story characters ask it for their quest markers, the portal asks it if it may open)
    static StoryDirector current;

    // the standing stone west of Brackenford (the World 2 portal) only wakes once Old Maud has spoken. false = always open
    public static boolean LOCK_WORLD2_PORTAL = true;

    private final StoryHost host;
    private final DialogueBox box = new DialogueBox();
    private final QuestLogScreen log = new QuestLogScreen();
    private final TabletScreen tablet = new TabletScreen();
    private final KeyLocker keys = new KeyLocker();
    private final Random rng = new Random();

    // what happens when the conversation closes
    private Enemy pendingEnemy;
    private boolean pendingBoss;
    private String pendingTag;
    private int pendingTablet;     // a world whose tablet was just completed (0 = none)

    private static final class Toast {
        final String text;
        final Color color;
        final int life;   // frames it stays up
        int t;
        Toast(String text, Color color, int life) { this.text = text; this.color = color; this.life = life; }
    }
    private final List<Toast> toasts = new ArrayList<>();

    private static final Color C_NEXT = new Color(240, 110, 100);
    private static final Color C_QUEST = new Color(255, 210, 58), C_DONE = new Color(150, 220, 150), C_PIECE = new Color(230, 200, 140);
    private static final Color[] C_FACTION = { new Color(220, 210, 180), new Color(220, 70, 70), new Color(130, 200, 120) };

    public StoryDirector(StoryHost host) {
        this.host = host;
        current = this;
        keys.lockKey(Key.Q);
        keys.lockKey(Key.ESC);
    }

    // ================================================================= every frame

    // runs the open overlay (conversation, tablet, quest log). Q opens the quest log when the knight is free to move.
    public boolean updateOverlays(boolean canOpenLog) {
        for (Toast t : toasts) t.t++;
        toasts.removeIf(t -> t.t > t.life);
        if (tablet.isOpen()) {
            if (!tablet.update()) afterOverlay();
            return true;
        }
        if (box.isOpen()) {
            box.update();
            return true;
        }
        if (log.isOpen()) {
            int view = log.update();
            if (view > 0) { tablet.open(view, false); return true; }   // ENTER on a tablet in the log
            if (!log.isOpen()) afterOverlay();
            return true;
        }
        if (Keyboard.isKeyUp(Key.Q)) keys.unlockKey(Key.Q);
        if (canOpenLog && Keyboard.isKeyDown(Key.Q) && !keys.isKeyLocked(Key.Q)) {
            keys.lockKey(Key.Q);
            host.getStoryPlayer().lock();
            log.open();
            return true;
        }
        return false;
    }

    public boolean isBusy() { return tablet.isOpen() || box.isOpen() || log.isOpen(); }

    // a conversation / tablet / the log has closed: start the fight it asked for, show the finished tablet, or free the knight
    private void afterOverlay() {
        if (box.isOpen() || tablet.isOpen() || log.isOpen()) return;
        if (pendingEnemy != null) {
            Enemy e = pendingEnemy;
            pendingEnemy = null;
            host.startStoryBattle(e, pendingBoss, pendingTag);
            return;
        }
        if (pendingTablet > 0) {
            int w = pendingTablet;
            pendingTablet = 0;
            tablet.open(w, true);
            return;
        }
        if (log.wasOpen()) keys.lockKey(Key.Q);   // the Q that closed the log must not reopen it
        host.getStoryPlayer().unlock();
    }

    public void drawToasts(GraphicsHandler gh) {
        int W = ScreenManager.getScreenWidth(), y = 132;
        for (Toast t : toasts) {
            int a = Math.min(255, Math.min(t.t * 12, (t.life - t.t) * 6));
            if (a <= 0) continue;
            List<String> lines = BattleUI.wrap(t.text, Math.max(20, (W - 80) / 12));
            int w = 0;
            for (String l : lines) w = Math.max(w, BattleUI.textW(l, 2));
            w += 30;
            int x = W / 2 - w / 2, h = 12 + lines.size() * 20;
            Color c = new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), a);
            gh.drawFilledRectangle(x, y, w, h, new Color(14, 10, 12, a * 200 / 255));
            gh.drawRectangle(x, y, w, h, c, 1);
            for (int i = 0; i < lines.size(); i++) BattleUI.textCenter(gh, lines.get(i), W / 2, y + 8 + i * 20, 2, c);
            y += h + 6;
        }
    }

    public void drawOverlays(GraphicsHandler gh) {
        if (log.isOpen()) log.draw(gh);
        if (box.isOpen()) box.draw(gh);
        if (tablet.isOpen()) tablet.draw(gh);
    }

    private void toast(String text, Color c) { toasts.add(new Toast(text, c, 200)); }

    // a long notice (about 7 seconds) for where to go next
    private void toastLong(String text, Color c) { toasts.add(new Toast(text, c, 420)); }

    // ================================================================= quest helpers

    private void start(String id) {
        if (StoryState.started(id)) return;
        StoryState.setStage(id, 0);
        Quest q = Quest.get(id);
        toast((q.main ? "NEW QUEST: " : "SIDE QUEST: ") + q.title, C_QUEST);
    }

    private void stage(String id, int s) {
        StoryState.setStage(id, s);
        Quest q = Quest.get(id);
        if (q != null && s < q.objectives.length) toast(Quest.progress(q, q.objectives[s]), new Color(220, 214, 196));
    }

    private void complete(String id) {
        if (StoryState.done(id)) return;
        StoryState.setStage(id, StoryState.DONE);
        Quest q = Quest.get(id);
        toast("QUEST COMPLETE: " + q.title, C_DONE);
        Player p = host.getStoryPlayer();
        int xp = (int) Math.max(5, Balance.xpToNextLevel(p.getLevel()) * (q.main ? 0.6 : 0.35));
        p.addXP(xp);
        toast("+" + xp + " EXP", C_DONE);
        // a main quest is done: where the story goes next (also shown in the journal)
        String next = Quest.nextAfter(id);
        if (next != null) toastLong("NEXT: " + next, C_NEXT);
    }

    private void gold(int n) {
        host.getStoryPlayer().addGold(n);
        toast("+" + n + " GOLD", C_QUEST);
    }

    // gives an item by its class name (the item packages differ, so each likely one is tried)
    private void item(String simpleName, String shownName) {
        String[] pkgs = { "Item.Sword.World1.", "Item.Sword.", "Item.Shield.World1.", "Item.Shield.", "Item.RangedWeapon.World1.",
                "Item.RangedWeapon.", "Item.Armor.World1.", "Item.Armor." };
        for (String p : pkgs) {
            try {
                Item it = Class.forName(p + simpleName).asSubclass(Item.class)
                        .getConstructor(GameObject.SpriteSheet.class).newInstance((Object) null);
                host.getInventory().addItem(it);
                toast("RECEIVED " + shownName, C_QUEST);
                return;
            } catch (ReflectiveOperationException | ClassCastException | LinkageError ignored) { }
        }
        System.err.println("Story: no item class named " + simpleName);
    }

    private void standing(int faction, int delta) {
        StoryState.shift(faction, delta);
        toast((delta > 0 ? "+" : "") + delta + " " + StoryState.FACTIONS[faction], C_FACTION[faction]);
    }

    private void choice(String line) { StoryState.logChoice(line); }

    private void piece(int world, int index) {
        if (!StoryState.givePiece(world, index)) return;
        toast((index == StoryState.KEYSTONE ? "THE KEYSTONE! " : "TABLET PIECE FOUND ") + StoryState.pieceCount(world) + "/6", C_PIECE);
        if (StoryState.tabletComplete(world)) pendingTablet = world;
    }

    private void fight(Enemy e, boolean boss, String tag) {
        pendingEnemy = e;
        pendingBoss = boss;
        pendingTag = tag;
    }

    private void show(Dialogue d) {
        host.getStoryPlayer().lock();
        d.onEnd(this::afterOverlay);
        box.open(d);
    }

    private static Dialogue talk(String who, Look look) { return new Dialogue(who, look); }

    private static Dialogue narration() { return new Dialogue("", null); }

    // ================================================================= events from the game

    public void onPlaceEntered(String place) {
        switch (place) {
            case "Barren Fields": if (StoryState.at("w1_salt", 0)) stage("w1_salt", 1); break;
            case "Dry River": if (StoryState.at("w1_water", 0)) stage("w1_water", 1); break;
            case "Barrows": if (StoryState.at("w1_graves", 0)) stage("w1_graves", 1); break;
            case "Ashwood": if (StoryState.at("w1_warden", 0)) stage("w1_warden", 1); break;
            default: break;
        }
    }

    // a battle was won. enemy = its name, place = the named place it was fought in (or null)
    public void onVictory(String enemy, String place) {
        String e = enemy == null ? "" : enemy.toUpperCase();
        if (e.equals("STARVING WRETCH")) {
            if (StoryState.at("w1_salt", 1) && "Barren Fields".equals(place)) {
                int n = StoryState.add("w1_salt_kills", 1);
                if (n >= 3) {
                    stage("w1_salt", 2);
                    piece(1, 0);
                    show(narration()
                            .narrate("AMONG THE LAST WRETCH'S RAGS: A WEDDING RING, ENGRAVED 'T + I'.")
                            .narrate("AND, TIED TO THE SAME STRING, A PIECE OF CARVED STONE.")
                            .as("KNIGHT", Look.KNIGHT).say("...Tomas."));
                } else toast("STARVING WRETCHES " + n + "/3", new Color(220, 214, 196));
            }
            if (StoryState.at("s1_bury", 0)) {
                int n = StoryState.add("s1_bury_tokens", 1);
                toast("WRETCH TOKENS " + Math.min(n, 5) + "/5", new Color(220, 214, 196));
                if (n >= 5) stage("s1_bury", 1);
            }
        }
        if (StoryState.at("w1_graves", 1) && "Barrows".equals(place)) {
            int n = StoryState.add("w1_graves_kills", 1);
            if (n >= 3) stage("w1_graves", 2);
            else toast("RECKONERS DRIVEN OFF " + n + "/3", new Color(220, 214, 196));
        }
    }

    // a fight the story started has ended
    public void onStoryBattleEnd(String tag, boolean won) {
        if (tag == null) return;
        if (tag.equals("w1_seed_cellar")) {
            if (!won) return;
            StoryState.setStage("w1_seed", 2);
            StoryState.set("wren_dead");
            show(narration().narrate("THE ZEALOT FALLS AMONG THE GRAIN SACKS. YOU TAKE ONE AND RUN.")
                    .run(() -> toast(Quest.get("w1_seed").objectives[2], new Color(220, 214, 196))));
        }
        if (tag.equals("w1_warden") && won) wardenFalls();
    }

    // the knight stepped through the standing stone (World 2 portal)
    static boolean portalOpen() {
        return !LOCK_WORLD2_PORTAL || StoryState.stage("w1_kept") >= 1;
    }

    static void onPortalUsed() {
        if (current != null && StoryState.at("w1_kept", 1)) current.complete("w1_kept");
    }

    // ================================================================= quest markers over heads

    // the marker over a story character: '!' (has a new quest) or '?' (part of a quest you are on) followed by that quest's
    // id, e.g. "!w1_ration". StoryNPC colours it RED for a main quest and BLUE for a side quest. null = no marker.
    static String markerFor(String id) {
        switch (id) {
            case "wren": return !StoryState.started("w1_ration") ? "!w1_ration" : StoryState.at("w1_ration", 1) ? "?w1_ration" : null;
            case "hester": return StoryState.at("w1_ration", 0) ? "?w1_ration" : StoryState.at("w1_seed", 2) ? "?w1_seed" : null;
            case "ida": return StoryState.done("w1_ration") && !StoryState.started("w1_salt") ? "!w1_salt" : StoryState.at("w1_salt", 2) ? "?w1_salt" : null;
            case "ansel": return StoryState.at("w1_seed", 0) || StoryState.at("w1_seed", 1) ? "?w1_seed" : StoryState.at("s1_tithe", 0) ? "?s1_tithe" : null;
            case "mirel": return !StoryState.started("s1_tithe") ? "!s1_tithe" : null;
            case "joss": return StoryState.at("s1_tithe", 0) && !StoryState.flag("w1_tithe_decided") ? "?s1_tithe" : null;
            case "vale": return !StoryState.started("s1_recruit") ? "!s1_recruit" : null;
            case "bess": return StoryState.at("s1_recruit", 1) ? "?s1_recruit" : null;
            case "elsa": return !StoryState.started("s1_bury") ? "!s1_bury" : StoryState.at("s1_bury", 1) ? "?s1_bury" : null;
            case "maud": return StoryState.at("w1_kept", 0) ? "?w1_kept" : null;
            case "hale": return StoryState.at("w1_graves", 2) ? "?w1_graves" : null;
            case "warden": return StoryState.at("w1_warden", 1) ? "?w1_warden" : null;
            case "mark": return StoryState.at("w1_water", 1) ? "?w1_water" : null;
            case "mound": return StoryState.at("w1_graves", 3) ? "?w1_graves" : null;
            case "wall": return !StoryState.started("s1_names") ? "!s1_names" : !StoryState.done("s1_names") ? "?s1_names" : null;
            case "cache": return !StoryState.hasPiece(1, 4) ? "?w1_tablet" : null;
            default: return null;
        }
    }

    // ================================================================= conversations

    public void openStoryDialogue(String id) {
        Dialogue d;
        switch (id) {
            case "wren": d = wren(); break;
            case "hester": d = hester(); break;
            case "ida": d = ida(); break;
            case "bess": d = bess(); break;
            case "joss": d = joss(); break;
            case "vale": d = vale(); break;
            case "ansel": d = ansel(); break;
            case "mirel": d = mirel(); break;
            case "maud": d = maud(); break;
            case "elsa": d = elsa(); break;
            case "hale": d = hale(); break;
            case "warden": d = warden(); break;
            case "grave": d = grave(); break;
            case "wall": d = wall(); break;
            case "mark": d = mark(); break;
            case "mound": d = mound(); break;
            case "cache": d = cache(); break;
            default: d = narration().narrate("..."); break;
        }
        show(d);
    }

    // ---- Wren, the girl in the square (THE LAST RATION)
    private Dialogue wren() {
        Dialogue d = talk("WREN", Look.WREN);
        if (!StoryState.started("w1_ration")) {
            return d.say("Sir knight? You walked through the square like you meant to stay. Nobody stays.")
                    .say("Ma says bread is a prayer now. This is our ration for the week.")
                    .say("Hester, by the well, hasn't eaten in four days. Ma would beat me if I gave it away.")
                    .say("But if YOU carried it... then it isn't me giving it. Is it?")
                    .choose("I'LL CARRY IT", () -> start("w1_ration"), b -> b.say("She's the one in grey. Please hurry."))
                    .choose("NOT NOW", null, b -> b.say("...She might not have four more days."));
        }
        if (StoryState.at("w1_ration", 0)) return d.say("Hester's the one in grey, by the well. Please hurry.");
        if (StoryState.at("w1_ration", 1)) {
            if (StoryState.flag("w1_ration_whole")) {
                d.say("She ate it? All of it?").say("Then... here. Half of mine. Ma won't know.")
                        .narrateThen("WREN PRESSES HALF A HARD LITTLE LOAF INTO YOUR HAND.", () -> StoryState.set("w1_wren_bread"));
            } else {
                d.say("Half is still something. Ma says something is the most anyone gets now.");
            }
            return d.run(() -> { complete("w1_ration"); item("RustedShortSword", "RUSTED SHORTSWORD"); gold(25); })
                    .say("Oh, and Ida keeps asking everyone about her husband. He went to the fields and didn't come home.")
                    .say("She's by the old cart. Maybe a knight could find him.");
        }
        if (StoryState.active("w1_seed")) {
            return d.say("Is that you, sir? Everything's so far away today.").say("The Chorus has grain. They'll give it. Won't they?");
        }
        return d.say("The preacher says we deserve the hunger.").say("I don't think I did anything.");
    }

    // ---- Hester, the widow by the well
    private Dialogue hester() {
        Dialogue d = talk("HESTER", Look.HESTER);
        if (StoryState.at("w1_ration", 0)) {
            return d.say("Is that... bread? Wren sent it? That child.")
                    .say("I can't take all of it. You've a long road. Can I?")
                    .choose("GIVE HER ALL OF IT", () -> { StoryState.set("w1_ration_whole"); choice("GAVE HESTER THE WHOLE RATION"); },
                            b -> b.say("All of it. God keep you, sir. Somebody should pray for that girl.")
                                    .run(() -> stage("w1_ration", 1)))
                    .choose("GIVE HER HALF", () -> { StoryState.set("w1_ration_half"); choice("GAVE HESTER HALF THE RATION"); },
                            b -> b.say("Half. Of course. Half is a kindness too, these days.").run(() -> stage("w1_ration", 1)));
        }
        if (StoryState.at("w1_seed", 2)) {
            d.say("You're too late, sir.").say("She went in the night. The fever took her quick, at least.");
            if (StoryState.flag("w1_wren_bread")) d.say("She'd saved her other half for you. Said the knight would need it more than she would.");
            else d.say("She kept asking if the knight came back. I told her you would.");
            return d.say("We buried her where she fell. By the well.")
                    .narrateThen("YOU GIVE HESTER THE GRAIN. SHE LOOKS AT IT FOR A LONG TIME.", () -> { complete("w1_seed"); gold(60); })
                    .say("She drew the river in the dirt, before the end. Over and over.")
                    .say("She said it didn't dry up. It went DOWN. Into the ground.")
                    .say("Find out what took it, sir. For her.")
                    .run(() -> start("w1_water"));
        }
        if (StoryState.flag("wren_dead")) return d.say("Follow the river, sir. South-east, past the fields.");
        if (StoryState.done("w1_ration")) return d.say("The well's been dry since spring. I come here anyway. Habit.");
        return d.say("Leave an old woman be, sir. I've nothing left to take.");
    }

    // ---- Ida, Tomas's wife (SALT IN THE FURROWS)
    private Dialogue ida() {
        Dialogue d = talk("IDA", Look.IDA);
        if (!StoryState.done("w1_ration")) return d.say("Not now. I'm waiting for Tomas. He'll be back by dark.");
        if (!StoryState.started("w1_salt")) {
            return d.say("You're the knight who helped Wren. Please.")
                    .say("My Tomas went to the Barren Fields to dig for roots. That was three days ago.")
                    .say("The hungry ones are out there. The Wretches. Please find him.")
                    .choose("I'LL FIND HIM", () -> start("w1_salt"), b -> b.say("South of the square. Bring him home."))
                    .choose("I CAN'T", null, b -> b.say("...No one can, anymore."));
        }
        if (StoryState.active("w1_salt") && StoryState.stage("w1_salt") < 2) return d.say("The fields are south of the square. Please.");
        if (StoryState.at("w1_salt", 2)) {
            return d.say("You're back. Did you... did you find him?")
                    .choose("TELL HER THE TRUTH", () -> { StoryState.set("w1_ida_truth"); choice("TOLD IDA HER HUSBAND HAD BECOME A WRETCH"); },
                            b -> b.narrate("YOU GIVE HER THE RING.")
                                    .say("He was one of them. The hungry ones. And you...")
                                    .say("...You did right. He'd have hated being that. He'd have hated it."))
                    .choose("SAY HE WENT SOUTH", () -> { StoryState.set("w1_ida_lie"); choice("TOLD IDA HER HUSBAND WENT SOUTH FOR WORK"); },
                            b -> b.say("South. Yes. He always said there was work in the south.")
                                    .say("He'll send for me. You'll see."))
                    .run(() -> { complete("w1_salt"); item("WarpedBucklerShield", "WARPED BUCKLER"); gold(40); })
                    .say("Sir... Wren took a fever last night. Bad.")
                    .say("The Hollow Chorus keeps seed grain in the cellars of the Granary Cloister, east along the road.")
                    .say("They won't open them for the likes of me. Maybe for a knight.")
                    .run(() -> start("w1_seed"));
        }
        if (StoryState.flag("w1_ida_truth")) return d.say("I buried his ring in the yard. It's something to visit.");
        return d.say("When he comes back I'll have the fire going. Every night, just in case.");
    }

    // ---- Brother Ansel of the Hollow Chorus (SEED FOR THE LAST WINTER, THE TITHE BASKET)
    private Dialogue ansel() {
        Dialogue d = talk("BROTHER ANSEL", Look.ANSEL);
        int h = StoryState.standing(StoryState.CHORUS);
        if (StoryState.at("s1_tithe", 0)) {
            d.say("The tithe. Sister Mirel sent you? Give it here.");
            if (StoryState.flag("w1_tithe_slipped"))
                d.say("...This basket is light, knight. The Chorus counts its bread.")
                        .run(() -> { standing(StoryState.CHORUS, -1); complete("s1_tithe"); gold(15); });
            else d.say("All of it. Good. The Chorus remembers those who carry.")
                    .run(() -> { standing(StoryState.CHORUS, 1); complete("s1_tithe"); gold(30); });
            if (!StoryState.active("w1_seed")) return d;
        }
        if (StoryState.at("w1_seed", 0)) {
            d.say(h >= 2 ? "Brother knight. The Chorus remembers your service." : "The hunger is a sermon, knight. Have you come to hear it?")
                    .say("Grain? For a fevered child? Everyone has a fevered child.")
                    .say("The seed in our cellars is for the LAST winter. When it comes, the faithful will eat. Kneel, and we will see.")
                    .choose("KNEEL AND ASK", () -> { standing(StoryState.CHORUS, 1); choice("KNELT TO THE HOLLOW CHORUS FOR GRAIN"); },
                            b -> b.say("Good. Humility first. The rite is about to begin. Wait through it.")
                                    .narrate("THE CHORUS SINGS FOR A LONG TIME. NOBODY SINGS ABOUT FOOD.")
                                    .run(() -> stage("w1_seed", 1)))
                    .choose("BREAK INTO THE CELLAR", () -> {
                                standing(StoryState.KEPT, 1);
                                standing(StoryState.CHORUS, -2);
                                choice("BROKE INTO THE CHORUS CELLAR");
                                fight(zealot(), false, "w1_seed_cellar");
                            },
                            b -> b.say("Then you are a thief, and the Chorus defends its altar."));
            return d;
        }
        if (StoryState.at("w1_seed", 1)) {
            return d.say("The rite is sung. The Chorus is merciful.")
                    .narrateThen("A ZEALOT DROPS A SMALL SACK OF SEED GRAIN AT YOUR FEET.", () -> {
                        StoryState.setStage("w1_seed", 2);
                        StoryState.set("wren_dead");
                        toast(Quest.get("w1_seed").objectives[2], new Color(220, 214, 196));
                    })
                    .say("Go quickly. Fevers do not wait for knights. Neither does the beast.");
        }
        if (h <= -2) return d.say("Thief. The Chorus remembers you too.");
        if (h >= 2) return d.say("Brother knight. When the last winter comes, there will be a place for you.");
        return d.say("The hunger is deserved, knight. Everything is deserved.");
    }

    // a Hollow Chorus Zealot at the knight's level (World 1's band), for the cellar
    private Enemy zealot() {
        Enemy z = new NPCs.World1.HollowChorusZealot(0, new Utils.Point(0, 0));
        int lvl = Balance.spawnLevel(1, Balance.Archetype.ELITE, host.getStoryPlayer().getLevel(), rng);
        return Balance.scale(z, Balance.Archetype.ELITE, lvl);
    }

    // ---- Sister Mirel collects the tithe in the square (THE TITHE BASKET)
    private Dialogue mirel() {
        Dialogue d = talk("SISTER MIREL", Look.MIREL);
        if (!StoryState.started("s1_tithe")) {
            return d.say("The rite needs bread, knight. The Chorus takes a tithe from every hearth that still has one.")
                    .say("Brother Ansel waits at the Granary Cloister. Carry the basket for me? I am needed here.")
                    .choose("CARRY IT", () -> start("s1_tithe"), b -> b.narrate("SHE HANDS YOU A BASKET OF SMALL, HARD LOAVES."))
                    .choose("NOT NOW", null, b -> b.say("The hungry will still be hungry later. So will the Chorus."));
        }
        if (StoryState.active("s1_tithe")) return d.say("Brother Ansel is at the Cloister, east along the road.");
        if (StoryState.flag("w1_tithe_slipped")) return d.say("Brother Ansel says the basket was light. I hope whoever ate it was hungry.");
        return d.say("The Chorus thanks you, knight.");
    }

    // ---- Joss, a starving father (the choice inside THE TITHE BASKET)
    private Dialogue joss() {
        Dialogue d = talk("JOSS", Look.JOSS);
        if (StoryState.at("s1_tithe", 0) && !StoryState.flag("w1_tithe_decided")) {
            return d.say("Is that bread? In a basket? For the Chorus?")
                    .say("My girls haven't eaten since... I don't remember since when.")
                    .choose("GIVE HIM A LOAF", () -> {
                                StoryState.set("w1_tithe_slipped");
                                StoryState.set("w1_tithe_decided");
                                standing(StoryState.KEPT, 1);
                                choice("GAVE A TITHE LOAF TO JOSS'S FAMILY");
                            },
                            b -> b.say("I... thank you. They'll never know it came from the Chorus. Good."))
                    .choose("KEEP WALKING", () -> { StoryState.set("w1_tithe_decided"); choice("KEPT THE TITHE BASKET WHOLE"); },
                            b -> b.say("No. Of course not. It's for the rite."));
        }
        if (StoryState.flag("w1_tithe_slipped")) return d.say("My girls ate today. First time this week. I won't forget it.");
        return d.say("Nothing to spare, knight. Nothing to sell either.");
    }

    // ---- Captain Vale of the Concord (RECRUITING THE HUNGRY)
    private Dialogue vale() {
        Dialogue d = talk("CAPTAIN VALE", Look.VALE);
        if (!StoryState.started("s1_recruit")) {
            return d.say("A knight! The Concord could use steel like yours.")
                    .say("We march against the Beast. A loaf of bread for every boy who signs on. They eat, the Beast dies. Fair trade.")
                    .say("Help me fill the ranks. The mothers listen to a knight.")
                    .choose("HELP HIM RECRUIT", () -> {
                                StoryState.setStage("s1_recruit", 0);
                                StoryState.set("w1_vale_helped");
                                standing(StoryState.CONCORD, 1);
                                choice("HELPED CAPTAIN VALE RECRUIT BOYS FOR BREAD");
                            },
                            b -> b.say("Good man. Three signed today. Here, your share.")
                                    .run(() -> { complete("s1_recruit"); gold(60); }))
                    .choose("WARN THE MOTHERS", () -> {
                                start("s1_recruit");
                                StoryState.setStage("s1_recruit", 1);
                                choice("WENT TO WARN THE MOTHERS ABOUT THE RECRUITER");
                            },
                            b -> b.say("Suit yourself, knight. The Beast won't kill itself."))
                    .choose("NOT NOW", null, b -> b.say("The offer stands. So does the Beast."));
        }
        if (StoryState.at("s1_recruit", 1)) return d.say("Seen any likely lads?");
        if (StoryState.flag("w1_vale_warned")) return d.say("Somebody's been talking to the mothers. Half my recruits vanished overnight.");
        int c = StoryState.standing(StoryState.CONCORD);
        return d.say(c >= 2 ? "Knight! The Concord won't forget you. When we march on the Hollow, you'll ride in front."
                : "The Beast burns hotter every year. Only the Concord stands against it.");
    }

    // ---- Bess, a mother in the square
    private Dialogue bess() {
        Dialogue d = talk("BESS", Look.BESS);
        if (StoryState.at("s1_recruit", 1)) {
            return d.say("Vale's bread? For my Tam? He's twelve.")
                    .say("...Thank you, sir. I'll keep him home. I'll tell the others.")
                    .run(() -> {
                        StoryState.set("w1_vale_warned");
                        standing(StoryState.KEPT, 1);
                        standing(StoryState.CONCORD, -1);
                        complete("s1_recruit");
                    });
        }
        if (StoryState.flag("w1_vale_warned")) return d.say("Tam's home. Hungry, but home.");
        if (StoryState.flag("w1_vale_helped")) return d.say("They took Tam yesterday. For one loaf. You were standing right there.");
        return d.say("Have you seen the recruiters? They took Mary's boy for one loaf of bread.");
    }

    // ---- Elsa of the Kept, in the Barren Fields (BURY THE WRETCHES)
    private Dialogue elsa() {
        Dialogue d = talk("ELSA", Look.ELSA);
        if (!StoryState.started("s1_bury")) {
            return d.say("Those things you've been killing, knight. The Wretches. They were farmers. Neighbours.")
                    .say("Bring me something from each one. A button, a ring, a thread. I'll give them graves.")
                    .choose("I WILL", () -> start("s1_bury"), b -> b.say("Five will do. Five is a start."))
                    .choose("NOT NOW", null, b -> b.say("They'll wait. They've nowhere else to be."));
        }
        if (StoryState.at("s1_bury", 0)) return d.say("Tokens: " + Math.min(5, StoryState.count("s1_bury_tokens")) + " of 5. Bring them when you have them.");
        if (StoryState.at("s1_bury", 1)) {
            return d.narrate("YOU GIVE HER FIVE SMALL THINGS: A BUTTON, A RING, A RIBBON, A WOODEN SPOON, A CHILD'S TOOTH.")
                    .say("They're people again. Here, at least.")
                    .run(() -> { standing(StoryState.KEPT, 1); choice("BURIED THE STARVING WRETCHES"); complete("s1_bury"); gold(60); });
        }
        return d.say("The graves are the only thing growing in these fields.");
    }

    // ---- Hale, a wounded Reckoner at the Barrows (GRAVES THAT WALK)
    private Dialogue hale() {
        Dialogue d = talk("HALE", Look.HALE);
        if (StoryState.at("w1_graves", 2)) {
            return d.say("Don't. I'm done fighting. Look at me.")
                    .say("You're hunting the beast? Fool. We don't serve the beast. We serve the Warden.")
                    .say("He takes the last of every harvest and says the beast is owed. Lives in Ashwood with his baskets.")
                    .say("There's something in the oldest mound. Glows at night. We don't go near it.")
                    .run(() -> stage("w1_graves", 3));
        }
        return d.say("Leave me to the graves, knight. I'll be in one soon enough.");
    }

    // ---- the Famine Warden in Ashwood (THE FAMINE WARDEN)
    private Dialogue warden() {
        Dialogue d = talk("THE FAMINE WARDEN", Look.WARDEN);
        if (!StoryState.at("w1_warden", 1)) return d.say("Not your harvest yet, knight. Come back hungrier.");
        return d.say("Another mouth.")
                .say("I take what is owed, knight. The last of every harvest. The beast in the river is owed, and I am its basket.")
                .choose("THEN TAKE IT FROM ME", () -> fight(EnemyFactory.boss("famine warden"), true, "w1_warden"),
                        b -> b.say("Gladly."))
                .choose("NOT YET", null, b -> b.say("Hunger keeps. I'll be here."));
    }

    // the Warden is beaten: he speaks (or not), the keystone, then Old Maud's quest
    private void wardenFalls() {
        Dialogue d = talk("THE FAMINE WARDEN", Look.WARDEN)
                .say("...Owed. It was all... owed.")
                .choose("LET HIM SPEAK", () -> { StoryState.set("w1_warden_spoke"); choice("LET THE FAMINE WARDEN SPEAK BEFORE HE DIED"); },
                        b -> b.say("You think I wanted this? I counted the baskets. Someone had to.")
                                .say("The beast counts too, knight. It counts EVERYTHING.")
                                .say("There's a stone in my coat. It came out of the river the day the water went. Read it. Then tell me I was wrong."))
                .choose("FINISH IT", () -> { StoryState.set("w1_warden_finished"); choice("FINISHED THE FAMINE WARDEN WITHOUT A WORD"); },
                        b -> b.narrate("THE WARDEN FALLS SILENT. A HEAVY CARVED STONE SLIPS FROM HIS COAT."))
                .narrateThen("THE KEYSTONE OF THE TABLET: THE LARGEST PIECE, CARVED WITH A DRAGON RISING FROM A RIVER.", () -> {
                    piece(1, StoryState.KEYSTONE);
                    complete("w1_warden");
                    item("FamineEdgeBlade", "FAMINE-EDGE BLADE");
                    gold(120);
                })
                .narrate("ITS CARVING SPEAKS OF A COMPACT OLDER THAN THE WALLS. THE KEPT GROVE, WEST OF BRACKENFORD, STILL REMEMBERS IT.")
                .run(() -> start("w1_kept"));
        show(d);
    }

    // ---- Old Maud of the Kept (THE KEPT REMEMBER)
    private Dialogue maud() {
        Dialogue d = talk("OLD MAUD", Look.MAUD);
        int k = StoryState.standing(StoryState.KEPT);
        if (StoryState.at("w1_kept", 0)) {
            d.say(k >= 2 ? "The Kept have heard of you, knight. You bury what others leave in the road." : "A knight. In the Grove. That's new.")
                    .say("You carry the Warden's stone. Let me see.");
            int n = StoryState.pieceCount(1);
            if (n < 6) d.say("Broken. " + n + " pieces of 6. Even broken, it says enough: the compact, the hunger, the judge.");
            else d.say("Whole. Then you have read the first word of the sentence.");
            return d.say("Long ago the people swore to take only what they needed. The beasts kept the water and the soil. The people broke it.")
                    .say("The gods did not strike us down. They sent a judge. The famine is its sentence.")
                    .say("The Kept still tend the old standing stone, west of town. It can show you what was done, if you promise to remember it.")
                    .choose("I SWEAR TO REMEMBER", () -> { StoryState.set("w1_sworn"); standing(StoryState.KEPT, 1); choice("SWORE TO OLD MAUD TO REMEMBER THE COMPACT"); },
                            b -> b.say("Then the stone will wake for you. Go west, knight. Mind what you see."))
                    .choose("I SWEAR NOTHING", () -> { StoryState.set("w1_silent"); choice("REFUSED TO SWEAR TO OLD MAUD"); },
                            b -> b.say("Honest, at least. The stone wakes for honest men too. Go west."))
                    .run(() -> stage("w1_kept", 1));
        }
        if (StoryState.at("w1_kept", 1)) return d.say("West, knight. The stone is awake.");
        if (StoryState.done("w1_kept")) return d.say(StoryState.flag("w1_sworn") ? "Remember, knight. You swore." : "Still here? The past won't wait forever.");
        return d.say(k >= 2 ? "The Kept remember kindness, knight. Few enough do." : "You've the look of someone carrying a question. Come back when you've carried it further.");
    }

    // ---- Wren's grave by the well
    private Dialogue grave() {
        Dialogue d = narration().narrate("A SMALL MOUND BY THE DRY WELL. A STICK CROSS. SOMEONE HAS SCRATCHED 'WREN' INTO IT.");
        if (StoryState.flag("w1_wren_bread")) d.narrate("ON THE GRAVE LIES HALF A HARD LITTLE LOAF. NOBODY HAS TAKEN IT.");
        return d;
    }

    // ---- the Wall of Names (NAMES ON THE WALL)
    private static final String[] NAMES = {
            "MARY ASHBY, 9. SHE GAVE HER SHARE TO HER LITTLE BROTHER. HE LIVED A WEEK LONGER.",
            "OLD PIET THE MILLER. HE GROUND ACORNS AND BARK UNTIL HIS HANDS GAVE OUT.",
            "'THE TWINS'. NO NAMES. THE CHORUS SAID UNNAMED CHILDREN ARE NOT OWED A PRAYER.",
            "GARRET COOPER. WENT TO THE CONCORD FOR BREAD. CAME HOME IN A CART.",
            "SISTER ODA OF THE CHORUS. SHE OPENED THE CELLAR ONCE. ONCE.",
            "A NAME SCRATCHED OUT. UNDER IT, SOMEONE HAS CARVED: HE STOLE." };

    private Dialogue wall() {
        Dialogue d = narration();
        if (!StoryState.started("s1_names")) {
            d.narrate("A STRETCH OF WALL COVERED IN SCRATCHED NAMES. THE FAMINE DEAD. SOME HAVE A FEW WORDS BESIDE THEM.");
            d.run(() -> start("s1_names"));
        }
        if (!StoryState.done("s1_names")) {
            int n = StoryState.count("s1_names_read");
            d.narrateThen(NAMES[Math.min(n, NAMES.length - 1)], () -> {
                int read = StoryState.add("s1_names_read", 1);
                if (read < NAMES.length) toast("NAMES READ " + read + "/6", new Color(220, 214, 196));
            });
            if (n + 1 >= NAMES.length)
                d.narrate("BEHIND THE LAST NAME A STONE IS LOOSE. SOMEONE HID A CARVED PIECE OF STONE BEHIND IT.")
                        .run(() -> { piece(1, 3); complete("s1_names"); choice("READ EVERY NAME ON THE WALL"); });
            return d;
        }
        d.narrate("THE WALL OF NAMES.");
        if (StoryState.flag("wren_dead")) d.narrate("SOMEONE HAS ADDED A NEW NAME, LOW DOWN, IN A CHILD'S HAND: WREN.");
        return d;
    }

    // ---- Dabbah's mark in the Dry River (WHERE THE WATER WENT)
    private Dialogue mark() {
        Dialogue d = narration();
        if (StoryState.at("w1_water", 1)) {
            return d.narrate("THE RIVERBED IS TORN OPEN FROM BELOW. THREE GOUGES, EACH WIDE ENOUGH TO LIE DOWN IN.")
                    .narrate("THE STONE AT THE BOTTOM IS STILL WARM.")
                    .as("???", null).glitch("...COUNTING. COUNTING. COUNTING...")
                    .as("", null).narrate("THE AIR SNAPS BACK. WHATEVER THAT WAS, IT IS GONE.")
                    .narrateThen("IN THE DEEPEST GOUGE LIES A CARVED PIECE OF STONE.", () -> {
                        piece(1, 1);
                        complete("w1_water");
                        item("CrackedHuntingBow", "CRACKED HUNTING BOW");
                        gold(50);
                    })
                    .narrate("GREAT CLAW MARKS LEAD AWAY FROM THE RIVER, NORTH, TOWARD THE OLD BURIAL MOUNDS.")
                    .run(() -> start("w1_graves"));
        }
        if (StoryState.done("w1_water")) return d.narrate("THE GOUGES STILL FEEL WARM. THEY ALWAYS WILL.");
        return d.narrate("CRACKED WHITE GROUND. SOMETHING ENORMOUS MOVED HERE, A LONG TIME AGO OR YESTERDAY.");
    }

    // ---- the oldest burial mound at the Barrows (GRAVES THAT WALK)
    private Dialogue mound() {
        Dialogue d = narration();
        if (StoryState.at("w1_graves", 3)) {
            return d.narrate("INSIDE THE MOUND, AMONG VERY OLD BONES, A CARVED STONE GLOWS FAINTLY.")
                    .narrateThen("IT SHOWS A HOODED FIGURE CARRYING A BASKET AWAY FROM A KNEELING VILLAGE.", () -> {
                        piece(1, 2);
                        complete("w1_graves");
                        item("ScavengersHideArmor", "SCAVENGER'S HIDE");
                        gold(70);
                    })
                    .narrate("THE WARDEN. ASHWOOD IS NORTH-WEST, ACROSS THE VALLEY.")
                    .run(() -> start("w1_warden"));
        }
        if (StoryState.done("w1_graves")) return d.narrate("THE MOUND IS QUIET NOW.");
        return d.narrate("AN OLD BURIAL MOUND. THE DOOR IS SEALED WITH STONE, BUT SOMETHING INSIDE GLINTS.");
    }

    // ---- the Kept's stone cache at Kept Grove
    private Dialogue cache() {
        Dialogue d = narration();
        if (!StoryState.hasPiece(1, 4)) {
            return d.narrate("A STONE BOX CARVED WITH AN OPEN EYE. INSIDE, WRAPPED IN MOSS, IS A CARVED PIECE OF STONE.")
                    .run(() -> piece(1, 4));
        }
        return d.narrate("THE BOX IS EMPTY. THE CARVED EYE IS STILL OPEN.");
    }
}