package Screens;

import BattleSystem.Battle;
import BattleSystem.BattleMath;
import Engine.*;
import Game.GameState;
import Game.ScreenCoordinator;
import Item.Armor.World1.FamineEdgeVestmentsArmor;
import Item.Consumable.Consumables;
import Item.RangedWeapon.World1.CrackedHuntingBow;
import Item.Shield.World1.PatchedWoodShield;
import Item.Shield.World3.BeastbaneBarrier;
import Item.Sword.World1.RustedShortSword;
import Item.Sword.World1.WraithboneSaber;
import Item.Sword.World2.BaronsBroadsword;
import Game.PlayerData;
import Level.*;
import Maps.BlankMap;
import Maps.GuildHallMap;
import Maps.World2GuildHallMap;
import Maps.TestMap;
import Maps.World1Map;
import Maps.World2Map;
import Maps.World3Map;
import NPCs.Enemy;
import NPCs.World1.Dabbah;
import Players.Knight;
import Item.Item;
import Save.Save;
import Save.SaveData;
import BattleSystem.BattleListener;
import Encounters.EnemyFactory;
import Locations.NamedLocations;
import BattleSystem.BattleWorld;
import BattleSystem.Balance;
import Screens.Battle.EncounterScreen;
import Screens.Battle.PlayBattleScreen;
import Sound.Sound;
import Sound.SoundManager;
import SpriteFont.MonoSpriteFont;
import SpriteFont.SpriteFont;
import Utils.Direction;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import inventory.Inventory;
import Chest.ChestController;
import Chest.ChestId;
import Chest.ChestOpener;
import Chest.ChestRegistry;
import Chest.ChestResult;
import Chest.ChestWorlds;
import Chest.MapChests;
import Shop.Blacksmith.BlacksmithOpener;
import Shop.GameClock;
import Shop.ShopController;
import Shop.ShopOpener;
import Shop.Merchant.Merchant;
import Shop.Merchant.MerchantScreen;
import Shop.Merchant.MerchantViewModel;
import Shop.Blacksmith.Blacksmith;
import Shop.Blacksmith.BlacksmithScreen;
import Shop.Blacksmith.BlacksmithViewModel;
import Survival.SurvivalController;
import Survival.SurvivalResult;
import Story.StoryDirector;
import Story.StoryHost;
import Story.StoryState;


// This class is for when the RPG game is actually being played
// BATTLE CHANGES (search for "BATTLE"): the battle fields, the BATTLE case in update()/draw(), currentMapName tracking in
// initialize()/onMapChange(), startBattle()/onBattleEnd(), the waypoint list (M) and place discovery.
// BALANCE CHANGES (search for "BALANCE:"): FIX the knight being rebuilt at level 1 on every map change (onMapChange),
// full HP after every battle (onBattleEnd), random enemies spawning near the knight's level (checkRandomEncounter), and
// World 4 fights inside Dabbah's Hollow (Balance.WORLD4_ZONE of the World 1 map).
// GLITCH CHANGES (search for "GLITCH:"): double battles (startBattle with a companion), and the T test key.
// WORLD MAP CHANGES (search for "WORLD MAP"): the M key opens WorldMapScreen; the WORLD_MAP state; travelTo()/closeWorldMap()
// and the getters the map screen reads.
// FINAL BOSS CHANGES (search for "FINAL BOSS:"): the Dabbah fight skips the encounter cut-scene (startBattle).
// CINEMATIC CHANGES (search for "CINEMATIC:"): the Dabbah test key is now G (was P). Test mode holds Dabbah at 1 HP, so every
// hit ends a form and plays its cinematic (the K key is gone).
// STORY CHANGES (search for "STORY:"): the World 1 story (package Story). StoryDirector runs the conversations with the story
// characters (World1Map places them), the quests, choices and faction standing, the quest journal (Q) and the hieroglyph
// tablets; while one of those is open the world waits. Battles report wins (kill quests) and story fights back to it,
// entering a named place can move a quest on, and the story is saved with the game (SaveData.story).
public class PlayLevelScreen extends Screen implements GameListener, BattleListener, ShopOpener, BlacksmithOpener,
        ChestOpener, StoryHost {
    protected ScreenCoordinator screenCoordinator;
    protected Map map;
    protected Player player;
    protected PlayLevelScreenState playLevelScreenState;
    protected WinScreen winScreen;
    protected FlagManager flagManager;
    protected AttackWindow attackWindow;
    protected KeyLocker keyLocker = new KeyLocker();
    protected SpriteFont coinCountDisplay;
    private InventoryScreen inventoryScreen;
    private WorldMapScreen worldMapScreen;   // WORLD MAP
    private Inventory inventory;
    //XP DISPLAY

    // SHOP: owns the shop's open/closed state and the game clock the merchant restocks by
    private ShopController shopController;
    // SHOP: the shop sells the stock of the last OUTDOOR map the player was in; the shared guild hall interior must not count (it would always be World 1)
    private String shopWorldMapName = "world1";
    // CHEST: where the chests are and which are opened live here (not on the map) for the same reason as the shop: onMapChange()
    // rebuilds the map. initialize() makes new ones, so a new game starts with every chest closed and in new places
    private ChestRegistry chestRegistry;
    private ChestController chestController;
    private Random chestRandom;
    // SURVIVAL: hunger and hydration. recreated in initialize(), so a new game (and resetLevel) refills them
    private SurvivalController survivalController;
    //XP DISPLAY TESTING
    private SpriteFont xpDisplay;
    private SpriteFont xpTillNextLevel;
    private int xp;
    //HEALTH BAR
    private SpriteFont healthDisplay;
    private SpriteFont currHealth;
    private SoundManager soundManager;


    // BATTLE: the running fight, and the name of the map the player is in (the battle uses it to pick its animated background)
    private PlayBattleScreen battleScreen;
    private String currentMapName = "world1";

    // ENCOUNTER / GAME OVER: the clash cut-scene that plays before a fight, the game-over screen, and the named-location memory
    private EncounterScreen encounterScreen;
    private GameOverScreen gameOverScreen;
    private Enemy pendingEnemy;
    private int pendingLevel;
    private boolean pendingBoss;
    private BattleWorld pendingWorld;
    private Enemy pendingCompanion;   // GLITCH: the second enemy of a double battle (null = 1-vs-1)
    private boolean pendingTest;      // GLITCH: TEST ONLY. The T key's battle: the knight takes no damage, every move unlocked
    private NamedLocations.Place lastPlace;     // the last named location the player was in (null until he enters one)
    private int bannerTimer;                    // frames left of the "place name" banner
    private boolean bannerNew;                 // the banner is for a newly discovered place
    private final Set<NamedLocations.Place> discovered = new HashSet<>();
    private static final int MAX_WAYPOINTS = 9;   // buttons shown at once (per world)
    private final MonoSpriteFont hudFont = new MonoSpriteFont();
    private int fadeIn;                         // frames left of the black fade-in after a respawn
    private final MonoSpriteFont bannerFont = new MonoSpriteFont();

    // STORY: the World 1 story, and what the last battle was (for kill quests and the fights the story starts)
    private StoryDirector story;
    private String storyBattleTag, battleEnemyName, battlePlace;

    // SAVE: K saves to the default slot, L loads it. saveMessage is shown for saveMessageTimer frames
    private String saveMessage;
    private int saveMessageTimer;

    public PlayLevelScreen(ScreenCoordinator screenCoordinator) {
        this.screenCoordinator = screenCoordinator;
    }

    public void initialize() {
        StoryState.reset();   // STORY: a new game starts a new story
        // setup state
        flagManager = new FlagManager();
        flagManager.addFlag("hasLostBall", false);
        flagManager.addFlag("hasTalkedToWalrus", false);
        flagManager.addFlag("hasTalkedToDinosaur", false);
        flagManager.addFlag("hasFoundBall", false);
        flagManager.addFlag("sawIntro", false);

        inventory = new Inventory();

        // CHEST: new registry and random for a new game. the controller gets the inventory with each open() call
        chestRegistry = new ChestRegistry();
        chestRandom = new Random();
        chestController = new ChestController(chestRegistry, chestRandom);

        // SURVIVAL: ticked once per frame while RUNNING, so it only drains while exploring
        survivalController = new SurvivalController(Config.TARGET_FPS);

        // define/setup map
        map = new World1Map();
        currentMapName = "world1";   // BATTLE
        shopWorldMapName = "world1";   // SHOP
        map.setFlagManager(flagManager);

        // setup player
        player = new Knight(map.getPlayerStartPosition().x, map.getPlayerStartPosition().y);
        player.setMap(map);
        playLevelScreenState = PlayLevelScreenState.RUNNING;
        player.setFacingDirection(Direction.LEFT);
        lastPlace = null;
        discovered.clear();
        CodexOfBeastsScreen.resetSeen();   // WORLD MAP: a new game starts with an empty bestiary
        stepDist = 0;
        stepsSinceBattle = 0;
        lastX = player.getX();
        lastY = player.getY();
        bannerTimer = 0;
        fadeIn = 0;
        trackPlace();

        map.setPlayer(player);

        // let pieces of map know which button to listen for as the "interact" button
        map.getTextbox().setInteractKey(player.getInteractKey());

        // add this screen as a "game listener" so other areas of the game that don't
        // normally have direct access to it (such as scripts) can "signal" to have it
        // do something
        // this is used in the "onWin" method -- a script signals to this class that the
        // game has been won by calling its "onWin" method
        map.addListener(this);

        // CHEST: add this map's chests before the scripts are preloaded, so their scripts get initialised too
        MapChests.install(map, currentMapName, chestRegistry, chestRandom);

        // preloads all scripts ahead of time rather than loading them dynamically
        // both are supported, however preloading is recommended
        map.preloadScripts();

        winScreen = new WinScreen(this);
        attackWindow = new AttackWindow(this);

        // initialize currency counter display
        coinCountDisplay = new SpriteFont("Gold: " + PlayerData.getCurrency(), 10, 50, "Arial", 24, Color.YELLOW);
        coinCountDisplay.setOutlineColor(Color.BLACK);
        coinCountDisplay.setOutlineThickness(2);
        inventoryScreen = new InventoryScreen(this);
        worldMapScreen = new WorldMapScreen(this);   // WORLD MAP
        story = new StoryDirector(this);              // STORY
        // SHOP: the merchant, the blacksmith and the clock live here (not on the map) because onMapChange() rebuilds the map and the player
        GameClock shopClock = new GameClock(Config.TARGET_FPS);
        Merchant merchant = new Merchant(shopClock, new Random());
        MerchantViewModel shopModel = new MerchantViewModel(merchant, () -> inventory);
        BlacksmithViewModel blacksmithModel = new BlacksmithViewModel(new Blacksmith(), () -> inventory);
        shopController = new ShopController(shopClock, merchant,
                new MerchantScreen(shopModel, () -> shopController.close()),
                new BlacksmithScreen(blacksmithModel, () -> inventory, () -> shopController.close()),
                () -> player.unlock());
        // TEMPORARY test inventory. gear uses direct constructors so the compiler checks it, consumables use the Consumables constants
        inventory.addItem(new RustedShortSword(null));
        inventory.addItem(new RustedShortSword(null));   // duplicate, to test the identity swap
        inventory.addItem(new WraithboneSaber(null));    // legendary, to test the color
        inventory.addItem(new CrackedHuntingBow(null));
        inventory.addItem(new PatchedWoodShield(null));
        inventory.addItem(new FamineEdgeVestmentsArmor(null));
        inventory.addItem(new BaronsBroadsword(null));   // world 2 item, to test the world 2 registrar
        inventory.addItem(new BeastbaneBarrier(null));   // world 3 item, to test the world 3 registrar
        // consumables take inventory slots (one per stack) but are not shown on the inventory screen yet
        for (int i = 0; i < 3; i++) inventory.addConsumable(Consumables.HARDTACK_RATION);
        for (int i = 0; i < 2; i++) inventory.addConsumable(Consumables.WATER_BOTTLE);
    }



    public void update() {
        // SAVE: if this level was started from the load screen, restore that save on the first update
        // (takePendingLoad returns null after the first call, so this only happens once)
        SaveData pending = screenCoordinator.takePendingLoad();
        if (pending != null) applySaveData(pending);
        if (inventory != null) inventory.removeBrokenItem();
        if (Keyboard.isKeyUp(Key.I)) keyLocker.unlockKey(Key.I);
        if (Keyboard.isKeyUp(Key.M)) keyLocker.unlockKey(Key.M);   // WORLD MAP
        if (Keyboard.isKeyUp(Key.T)) keyLocker.unlockKey(Key.T);   // GLITCH: test key
        if (Keyboard.isKeyUp(Key.G)) keyLocker.unlockKey(Key.G);   // CINEMATIC: Dabbah test key (was P)
        MouseInput.update();
        Point click = MouseInput.pollClick();   // a mouse click this frame, if any
        // SHOP: while the shop is open nothing else updates (no encounters, no clock ticks)
        if (shopController.isOpen()) { shopController.update(); return; }
        // STORY: a conversation, the journal (Q) or a tablet is open: the world waits. Q opens the journal while walking.
        if (story.updateOverlays(playLevelScreenState == PlayLevelScreenState.RUNNING && !player.isLocked() && fadeIn == 0)) return;
        // based on screen state, perform specific actions
        switch (playLevelScreenState) {
            // if level is "running" update player and map to keep game logic for the
            // platformer level going
            case RUNNING:
                // if I key is down and unlocked and the player isnt locked, lock it and initialize the inventory screen
                if (Keyboard.isKeyDown(Key.I) && (!keyLocker.isKeyLocked(Key.I) && !player.isLocked())) {
                    player.lock();
                    keyLocker.lockKey(Key.I);
                    inventoryScreen.initialize();
                    playLevelScreenState = PlayLevelScreenState.INVENTORY;
                    break;
                }
                // GLITCH: TEST KEY. T starts a World 4 double battle with the glitch entity, at the knight's level, anywhere.
                // Remove this block (and the T unlock above) before release.
                if (Keyboard.isKeyDown(Key.T) && !keyLocker.isKeyLocked(Key.T) && !player.isLocked() && fadeIn == 0) {
                    keyLocker.lockKey(Key.T);
                    EnemyFactory.Encounter test = EnemyFactory.testDoubleBattle(player.getLevel(), encRng);
                    startBattle(test.enemy, 1, test.boss, BattleWorld.WORLD_4, test.companion);
                    pendingTest = true;   // no damage to the knight, all moves unlocked
                    break;
                }
                // CINEMATIC: TEST KEY (was P). G starts the Dabbah fight anywhere, in test mode: the knight takes no damage and has
                // every move; Dabbah is held at 1 HP, so every hit ends a form and its cinematic plays.
                // Remove this block (and the G unlock above) before release.
                if (Keyboard.isKeyDown(Key.G) && !keyLocker.isKeyLocked(Key.G) && !player.isLocked() && fadeIn == 0) {
                    keyLocker.lockKey(Key.G);
                    startBattle(new Dabbah(0, new Utils.Point(0, 0)), 20, true, BattleWorld.WORLD_4, null);
                    battleScreen.setTestMode(true);
                    break;
                }
                // WORLD MAP: M opens the world map (not during a textbox / cut-scene, and not while fading in after a jump)
                if (Keyboard.isKeyDown(Key.M) && !keyLocker.isKeyLocked(Key.M) && !player.isLocked() && fadeIn == 0) {
                    player.lock();
                    keyLocker.lockKey(Key.M);
                    worldMapScreen.initialize();
                    playLevelScreenState = PlayLevelScreenState.WORLD_MAP;
                    break;
                }
                // waypoints: click a JUMP button on the right of the screen to travel to that place
                if (click != null && !player.isLocked() && fadeIn == 0) {
                    List<NamedLocations.Place> list = waypointList();
                    for (int n = 0; n < list.size(); n++) {
                        if (waypointButton(n).contains(click) && list.get(n) != placeHere()) {
                            moveToPlace(list.get(n));
                            break;
                        }
                    }
                }
                if (playLevelScreenState != PlayLevelScreenState.RUNNING) break;
                // SAVE: quick save / quick load
                if (Keyboard.isKeyDown(Key.K) && !keyLocker.isKeyLocked(Key.K) && !player.isLocked()) {
                    keyLocker.lockKey(Key.K);
                    showSaveMessage(saveGame(Save.DEFAULT_SLOT) ? "GAME SAVED" : "SAVE FAILED");
                }
                if (Keyboard.isKeyUp(Key.K)) keyLocker.unlockKey(Key.K);
                if (Keyboard.isKeyDown(Key.L) && !keyLocker.isKeyLocked(Key.L) && !player.isLocked()) {
                    keyLocker.lockKey(Key.L);
                    showSaveMessage(loadGame(Save.DEFAULT_SLOT) ? "GAME LOADED" : "NO SAVE TO LOAD");
                }
                if (Keyboard.isKeyUp(Key.L)) keyLocker.unlockKey(Key.L);
                if (saveMessageTimer > 0) saveMessageTimer--;
                player.update();
                map.update(player);
                shopController.tick();   // SHOP: game time only passes while exploring
                survivalTick();          // SURVIVAL
                trackPlace();
                checkRandomEncounter();
                if (fadeIn > 0) fadeIn--;
                if (bannerTimer > 0) bannerTimer--;
                coinCountDisplay.setText("Gold: " + PlayerData.getCurrency());
                // Opens attack window at random - I WAS TOLD I AM NOT ALLOWED TO REMOVE THIS!
                if(Keyboard.isKeyDown(Key.W) && !keyLocker.isKeyLocked(Key.W)){
                    keyLocker.lockKey(Key.W);
                    int randomNum = (int) Math.round(Math.random() * 100/5);
                    //System.out.println(randomNum);
                    if(randomNum == 20){ //REMOVE LATER
                        int randomNum2 = (int) Math.round(Math.random() * 100/5);
                        if(randomNum2 == 20){
                            //System.out.println(randomNum); // REMOVE LATER
                            playLevelScreenState = PlayLevelScreenState.ATTACK_WINDOW;
                        }
                    }
                }
                if(Keyboard.isKeyUp(Key.W)){
                    keyLocker.unlockKey(Key.W);
                }

                //XP Display
                xp = player.getCurrentXP();
                xpDisplay = new SpriteFont("Player XP:" ,10,6,"Arial",16,Color.WHITE);
                xpTillNextLevel = new SpriteFont(xp + "/" + player.getXpToNextLevel(),400,6,"Arial",16,Color.WHITE);

                healthDisplay = new SpriteFont("Player HP:" ,10,22,"Arial",16,Color.WHITE);
                currHealth = new SpriteFont(player.getCurrentHP() +" ",400,22,"Arial",16,Color.WHITE);

                break;
            // if level has been completed, bring up level cleared screen
            case LEVEL_COMPLETED:
                winScreen.update();
                break;
            case INVENTORY:
                // this ensures that animations still happen while inside the inventory
                player.update();
                map.update(player);
                inventoryScreen.update();
                break;
            // WORLD MAP: the world is frozen behind the map; the map screen gets this frame's click
            case WORLD_MAP:
                worldMapScreen.update(click);
                break;
            //If the game state was changed to attach window then the attack window should open
            case ATTACK_WINDOW:
                attackWindow.update();
                break;
            // BATTLE: the map and player are frozen while the fight plays
            case BATTLE:
                battleScreen.update();
                break;

            // ENCOUNTER: the knight and the enemy clash, then the battle begins
            case ENCOUNTER:
                encounterScreen.update();
                if (encounterScreen.isDone()) {
                    syncGearHealth();
                    battleScreen = new PlayBattleScreen(player, inventory, pendingEnemy, pendingLevel, pendingWorld, this);
                    battleScreen.setBoss(pendingBoss);
                    battleScreen.setCompanion(pendingCompanion);   // GLITCH
                    battleScreen.setTestMode(pendingTest);         // GLITCH: TEST ONLY (T key)
                    battleScreen.initialize();
                    soundManager.playSoundEffect(Sound.CLUNK); // Plays Clunk Music
                    soundManager.stopMusic(); // Pause other music
                    playLevelScreenState = PlayLevelScreenState.BATTLE;
                }
                break;
            // GAME_OVER: after ENTER the knight is put back at the last named location
            case GAME_OVER:
                gameOverScreen.update();
                if (gameOverScreen.isDone()) respawn();
                break;
        }
        //Check to see current state of the game or what map to set the background music
        soundManager.backgroundMusic(currentMapName,playLevelScreenState);
    }

    // BATTLE: starts a fight against the given enemy. The background is picked from the map the player is standing in.
    // Call this from wherever an encounter should happen. Pass boss = true for bosses (no running away, 2 heals).
    public void startBattle(Enemy enemy, int enemyLevel, boolean boss) {
        startBattle(enemy, enemyLevel, boss, BattleWorld.fromMapName(currentMapName));
    }

    // same, but with the battle background chosen by the caller
    public void startBattle(Enemy enemy, int enemyLevel, boolean boss, BattleWorld world) {
        startBattle(enemy, enemyLevel, boss, world, null);
    }

    // GLITCH: same, with a second enemy (a double battle); companion null = a normal 1-vs-1 fight
    public void startBattle(Enemy enemy, int enemyLevel, boolean boss, BattleWorld world, Enemy companion) {
        player.lock();
        storyBattleTag = null;   // STORY: startStoryBattle sets it again after this call
        battleEnemyName = enemy.getEnemyName();
        NamedLocations.Place fightPlace = placeHere();
        battlePlace = fightPlace != null ? fightPlace.name : null;
        CodexOfBeastsScreen.markSeen(enemy.getEnemyName());   // WORLD MAP: the bestiary unlocks an enemy once it is fought
        if (companion != null) CodexOfBeastsScreen.markSeen(companion.getEnemyName());
        pendingCompanion = companion;
        pendingTest = false;   // only the T key turns test mode on (after this call)
        // FINAL BOSS: Dabbah skips the clash cut-scene; the battle opens with Dabbah rising out of the earth, always in World 4
        if (enemy instanceof Dabbah && companion == null) {
            syncGearHealth();
            battleScreen = new PlayBattleScreen(player, inventory, enemy, enemyLevel, BattleWorld.WORLD_4, this);
            battleScreen.setBoss(true);
            battleScreen.initialize();
            playLevelScreenState = PlayLevelScreenState.BATTLE;
            return;
        }
        pendingEnemy = enemy;
        pendingLevel = enemyLevel;
        pendingBoss = boss;
        pendingWorld = world;
        encounterScreen = new EncounterScreen(world, enemy.getEnemyName());   // plays first; the battle starts when it finishes
        encounterScreen.initialize();
        playLevelScreenState = PlayLevelScreenState.ENCOUNTER;
    }

    // remembers the named location the player is standing in (and shows its banner when it changes)
    private void trackPlace() {
        NamedLocations.Place p = NamedLocations.find(currentMapName, player.getX(), player.getY());
        if (p != null && p != lastPlace) {
            lastPlace = p;
            bannerNew = discovered.add(p);   // first time here: the place becomes a waypoint
            bannerTimer = 150;
            if (story != null) story.onPlaceEntered(p.name);   // STORY: "search the Barren Fields" and the like
        }
    }

    // moves the knight to a named place's fixed tile (switching maps if it is in another world; the same knight goes along,
    // so level, XP, HP and stats are kept) and fades the world back in
    private void moveToPlace(NamedLocations.Place p) {
        if (p == null) {
            player.setLocation(map.getPlayerStartPosition().x, map.getPlayerStartPosition().y);
        } else if (!p.map.equals(currentMapName)) {
            float[] sp = NamedLocations.spawnPixels(p, buildMap(p.map));
            onMapChange(p.map, sp[0], sp[1]);
        } else {
            float[] sp = NamedLocations.spawnPixels(p, map);
            player.setLocation(sp[0], sp[1]);
        }
        player.unlock();
        if (p != null) { lastPlace = p; discovered.add(p); }
        bannerNew = false;
        stepDist = 0;
        stepsSinceBattle = 0;
        lastX = player.getX();
        lastY = player.getY();
        bannerTimer = 150;
        fadeIn = 60;
        playLevelScreenState = PlayLevelScreenState.RUNNING;
    }

    // after a defeat: back to the last named location with full health
    private void respawn() {
        soundManager.playSoundEffect(Sound.PLAYER_DIES); // Plays DIED sound
        syncGearHealth();
        player.setCurrentHP(player.getMaxHP());
        moveToPlace(lastPlace);
    }

    // ---- random encounters: every full tile the knight walks outside a SAFE place may start a fight ----
    private static final int GRACE_STEPS = 30;           // no fight until this many steps after the last one
    private static final double STEP_CHANCE = 0.90;     // chance per step after the grace period
    private float lastX, lastY, stepDist;
    private int stepsSinceBattle;
    private final Random encRng = new Random();

    private void checkRandomEncounter() {
        float dx = player.getX() - lastX, dy = player.getY() - lastY;
        lastX = player.getX();
        lastY = player.getY();
        if (Math.abs(dx) > 400 || Math.abs(dy) > 400) return;     // teleport (portal / respawn), not walking
        stepDist += (float) Math.sqrt(dx * dx + dy * dy);
        if (stepDist < NamedLocations.TILE_PX) return;
        stepDist -= NamedLocations.TILE_PX;
        stepsSinceBattle++;
        // no fights inside the safe zone (Brackenford's plaza); roads and everything else outside it can have them
        if (NamedLocations.isSafe(currentMapName, player.getX(), player.getY()) || stepsSinceBattle < GRACE_STEPS) return;
        if (encRng.nextDouble() >= STEP_CHANCE) return;
        stepsSinceBattle = 0;
        // a real enemy for this world, picked by spawn rate; the enemy class supplies its own level, stats and moves
        // BALANCE: the knight's level is passed in so the enemy spawns near it (clamped to the world's level band)
        // BALANCE: inside Dabbah's Hollow (part of the World 1 map, Balance.WORLD4_ZONE) the fights are World 4's
        boolean hollow = Balance.inWorld4(currentMapName, player.getX(), player.getY(), NamedLocations.TILE_PX);
        BattleWorld fightWorld = hollow ? BattleWorld.WORLD_4 : BattleWorld.fromMapName(currentMapName);
        EnemyFactory.Encounter enc = EnemyFactory.randomEncounter(fightWorld, encRng, player.getLevel());
        if (enc == null) return;   // this world has no random encounters yet
        stepsSinceBattle = 0;
        startBattle(enc.enemy, 1, enc.boss, fightWorld, enc.companion);   // BALANCE: World 4's backdrop; GLITCH: maybe a second enemy
    }

    // BATTLE: the battle calls this when it has finished fading out
    @Override
    public void onBattleEnd(Battle.BattleResult result) {
        player.unlock();
        playLevelScreenState = PlayLevelScreenState.RUNNING;
        // BALANCE: HP goes back to full after EVERY battle (PP refills on its own: each battle starts with full PP)
        syncGearHealth();
        player.setCurrentHP(player.getMaxHP()); // THIS LINE MAKES IT SO YOUR HP DOES NOT GO DOWN, SO THE HP BAR WAS WORKING IT WAS THE HP ITSELF WAS NOT GOING DOWN
        if (result == Battle.BattleResult.DEFEAT) {
            // the knight fell: show the game-over screen, which then sends him back to the last named location
            player.lock();
            gameOverScreen = new GameOverScreen(lastPlace != null ? lastPlace.name : "the start");
            gameOverScreen.initialize();
            playLevelScreenState = PlayLevelScreenState.GAME_OVER;
        }
        // VICTORY: XP and gold were already given by the battle.  FLED: nothing to do.
        // STORY: kill counts for quests, and what happens after a fight the story started
        String tag = storyBattleTag;
        storyBattleTag = null;
        if (result == Battle.BattleResult.VICTORY) story.onVictory(battleEnemyName, battlePlace);
        story.onStoryBattleEnd(tag, result == Battle.BattleResult.VICTORY);
    }

    // ---- STORY: what the story asks of this screen (StoryHost) ----
    @Override
    public void openStoryDialogue(String storyId) {
        story.openStoryDialogue(storyId);
    }

    @Override
    public void startStoryBattle(Enemy enemy, boolean boss, String tag) {
        if (enemy == null) { player.unlock(); return; }
        startBattle(enemy, 1, boss, BattleWorld.fromMapName(currentMapName));
        storyBattleTag = tag;
    }

    @Override
    public Player getStoryPlayer() {
        return player;
    }

    @Override
    public void onWin() {
        // when this method is called within the game, it signals the game has been
        // "won"
        playLevelScreenState = PlayLevelScreenState.LEVEL_COMPLETED;
    }

    @Override
    // Called when the player changes maps. The mapName is the name of the new map
    // and playerX and playerY are the player's new coordinates in that map.
    public void onMapChange(String mapName, float playerX, float playerY) {
        map = buildMap(mapName);
        currentMapName = mapName;   // BATTLE
        if (!"guild_hall_map".equals(mapName)) shopWorldMapName = mapName;   // SHOP
        map.setFlagManager(flagManager);
        Player previous = player;
        player = new Knight(playerX, playerY);
        player.copyProgressFrom(previous);   // BALANCE: FIX. Was a brand-new level 1 knight (100 HP) after every map change
        player.setMap(map);
        player.setLocation(playerX, playerY);
        player.unlock();
        player.setFacingDirection(Direction.LEFT);
        lastX = playerX;
        lastY = playerY;
        stepDist = 0;
        map.setPlayer(player);
        map.getTextbox().setInteractKey(player.getInteractKey());
        map.addListener(this);
        MapChests.install(map, currentMapName, chestRegistry, chestRandom);   // CHEST: before preloadScripts
        map.preloadScripts();
        trackPlace();
    }

    // creates the maps added for world 3 and the world 2 guild hall, and hands every other name to createMap
    private Map buildMap(String mapName) {
        switch (mapName) {
            case "world3_map":
                return new World3Map();
            case "guild_hall_map_world2":
                return new World2GuildHallMap();
            default:
                return createMap(mapName);
        }
    }

    // This method creates a new map based on the provided map name. It returns a
    // new instance of the appropriate map class.
    private Map createMap(String mapName) {
        return switch (mapName) {
            case "blank" -> new BlankMap();
            case "world1" -> new World1Map();
            case "world2_map" -> new World2Map();
            case "guild_hall_map" -> new GuildHallMap();
            default -> new TestMap();
        };
    }

    public void draw(GraphicsHandler graphicsHandler) {
        // SHOP: the shop window is drawn over the map
        if (shopController.isOpen()) { map.draw(player, graphicsHandler); shopController.draw(graphicsHandler); return; }
        // based on screen state, draw appropriate graphics
        switch (playLevelScreenState) {
            case RUNNING:
                map.draw(player, graphicsHandler);
                coinCountDisplay.draw(graphicsHandler);
                //DISPLAY XP
                xpDisplay.draw(graphicsHandler);
                xpTillNextLevel.draw(graphicsHandler);
                graphicsHandler.drawFilledRectangle(87,10,30,10,new Color(204,255,204));
                graphicsHandler.drawFilledRectangle(117,10,30,10,new Color(188, 246, 188));
                graphicsHandler.drawFilledRectangle(147,10,30,10,new Color(165, 243, 165));
                graphicsHandler.drawFilledRectangle(177,10,30,10,new Color(141, 244, 141));
                graphicsHandler.drawFilledRectangle(207,10,30,10,new Color(113, 244, 113));
                graphicsHandler.drawFilledRectangle(237,10,30,10,new Color(93, 246, 93));
                graphicsHandler.drawFilledRectangle(267,10,30,10,new Color(66, 243, 66));
                graphicsHandler.drawFilledRectangle(297,10,30,10,new Color(49, 243, 49, 216));
                graphicsHandler.drawFilledRectangle(327,10,30,10,new Color(24, 243, 24, 163));
                graphicsHandler.drawFilledRectangle(357,10,30,10,new Color(0, 255, 0, 115));
                int xpNeeded = player.getXpToNextLevel();   // 0 at max level, which counts as a full bar
                int xpFill = xpNeeded > 0 ? 300 * player.getCurrentXP() / xpNeeded : 300;
                graphicsHandler.drawFilledRectangle(87 + xpFill, 10, 300 - xpFill, 10, Color.LIGHT_GRAY);
                //HEALTH BAR
                healthDisplay.draw(graphicsHandler);
                currHealth.draw(graphicsHandler);
                float hpPercent = player.getCurrentHP() / player.getMaxHP() * 100;
                if(hpPercent  >= 10) {
                    graphicsHandler.drawFilledRectangle(87, 28, 30, 10, new Color(255, 59, 54));
                }
                if(hpPercent  >= 20) {
                    graphicsHandler.drawFilledRectangle(117, 28, 30, 10, new Color(246, 27, 24));
                }
                if(hpPercent >= 30) {
                    graphicsHandler.drawFilledRectangle(147, 28, 30, 10, new Color(255, 17, 13, 200));
                }
                if(hpPercent >= 40) {
                    graphicsHandler.drawFilledRectangle(177, 28, 30, 10, new Color(241, 169, 15, 236));
                }
                if(hpPercent >= 50) {
                    graphicsHandler.drawFilledRectangle(207, 28, 30, 10, new Color(253, 154, 73));
                }
                if(hpPercent >= 60) {
                    graphicsHandler.drawFilledRectangle(237, 28, 30, 10, new Color(145, 248, 145, 158));
                }
                if(hpPercent >= 70) {
                    graphicsHandler.drawFilledRectangle(267, 28, 30, 10, new Color(66, 243, 66, 165));
                }
                if(hpPercent >= 80) {
                    graphicsHandler.drawFilledRectangle(297, 28, 30, 10, new Color(85, 246, 85, 152));
                }
                if(hpPercent  >= 90) {
                    graphicsHandler.drawFilledRectangle(327, 28, 30, 10, new Color(48, 255, 48, 84));
                }
                if(hpPercent >= 100) {
                    graphicsHandler.drawFilledRectangle(357, 28, 30, 10, new Color(0, 255, 0, 71));
                }
                drawSurvivalBars(graphicsHandler);   // SURVIVAL
                drawWaypointButtons(graphicsHandler);   // JUMP buttons on the right
                drawPlaceBanner(graphicsHandler);       // place name when entering a location
                drawSaveMessage(graphicsHandler);       // SAVE
                story.drawToasts(graphicsHandler);      // STORY: "NEW QUEST", "+1 KEPT GROVE", ...
                story.drawOverlays(graphicsHandler);    // STORY: the conversation, the journal, a tablet
                break;
            case LEVEL_COMPLETED:
                winScreen.draw(graphicsHandler);
                break;
            case ATTACK_WINDOW:
                map.draw(player, graphicsHandler);
                attackWindow.draw(graphicsHandler);
                break;
            case INVENTORY:
                inventoryScreen.draw(graphicsHandler);
                break;
            case WORLD_MAP:   // WORLD MAP: the frozen world shows dimmed behind the map
                map.draw(player, graphicsHandler);
                worldMapScreen.draw(graphicsHandler);
                break;
            case BATTLE:   // BATTLE
                battleScreen.draw(graphicsHandler);
                break;
            case ENCOUNTER:
                encounterScreen.draw(graphicsHandler);
                break;
            case GAME_OVER:
                gameOverScreen.draw(graphicsHandler);
                break;
        }
    }

    // the discovered places of the world the knight is in (each world has its own buttons; going back to a world shows
    // its buttons again)
    private List<NamedLocations.Place> waypointList() {
        List<NamedLocations.Place> out = new ArrayList<>();
        for (NamedLocations.Place p : NamedLocations.all())
            if (p.map.equals(currentMapName) && discovered.contains(p) && out.size() < MAX_WAYPOINTS) out.add(p);
        return out;
    }

    // where the i-th JUMP button is on the screen
    private Rectangle waypointButton(int i) {
        int size = 34, gap = 8;
        return new Rectangle(ScreenManager.getScreenWidth() - size - 14, 70 + i * (size + gap), size, size);
    }

    // the JUMP buttons down the right side of the screen: click one to travel there.
    // A new button appears the first time the knight discovers a place; the one for the current place is lit green.
    private void drawWaypointButtons(GraphicsHandler gh) {
        List<NamedLocations.Place> list = waypointList();
        if (list.size() < 1) return;   // shown from the first discovered place
        Graphics2D g = gh.getGraphics();
        Rectangle first = waypointButton(0);
        hudFont.draw(gh, "JUMP", first.x + first.width / 2 - hudFont.getTextWidth("JUMP", 2, 1) / 2, first.y - 22, 2, 1, new Color(255, 240, 200));
        for (int i = 0; i < list.size(); i++) {
            NamedLocations.Place p = list.get(i);
            Rectangle r = waypointButton(i);
            boolean here = p == placeHere();
            boolean hover = r.contains(MouseInput.getX(), MouseInput.getY());
            g.setColor(here ? new Color(70, 150, 80, 230) : (hover ? new Color(90, 72, 40, 235) : new Color(30, 26, 22, 220)));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(here ? new Color(190, 240, 190) : (hover ? new Color(255, 230, 160) : new Color(214, 178, 110)));
            g.drawRect(r.x, r.y, r.width, r.height);
            String n = String.valueOf(i + 1);
            hudFont.draw(gh, n, r.x + r.width / 2 - hudFont.getTextWidth(n, 3, 1) / 2, r.y + r.height / 2 - hudFont.getCapHeight(3) / 2, 3, 1, Color.WHITE);
            {   // the place name beside its button (brighter when you point at it)
                String name = p.name.toUpperCase();
                int tw = hudFont.getTextWidth(name, 1, 1);
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRect(r.x - tw - 14, r.y + r.height / 2 - 8, tw + 10, 16);
                hudFont.draw(gh, name, r.x - tw - 9, r.y + r.height / 2 - hudFont.getCapHeight(1) / 2, 1, 1, here ? new Color(190, 240, 190) : new Color(255, 240, 200));
            }
        }
    }

    // the place name that slides in at the top when the knight enters a named location
    private void drawPlaceBanner(GraphicsHandler gh) {
        if (bannerTimer <= 0 || lastPlace == null) return;
        int a = Math.min(255, Math.min(bannerTimer * 8, (150 - bannerTimer) * 8));
        if (a <= 0) return;
        String name = lastPlace.name.toUpperCase();
        int w = bannerFont.getTextWidth(name, 3, 2);
        int cx = ScreenManager.getScreenWidth() / 2;
        Graphics2D g = gh.getGraphics();
        g.setColor(new Color(0, 0, 0, a * 140 / 255));
        g.fillRect(cx - w / 2 - 24, 56, w + 48, bannerNew ? 64 : 44);
        bannerFont.draw(gh, name, cx - w / 2, 68, 3, 2, new Color(255, 235, 190, a));
        if (bannerNew) {
            String tag = "NEW WAYPOINT UNLOCKED";
            bannerFont.draw(gh, tag, cx - bannerFont.getTextWidth(tag, 1, 1) / 2, 100, 1, 1, new Color(150, 220, 150, a));
        }
    }

    // SAVE: writes the current game to the given slot. Returns true if it was written
    public boolean saveGame(int slot) {
        return Save.save(slot, createSaveData());
    }

    // SAVE: replaces the current game with the one in the given slot. Returns false (and changes nothing) if the
    // slot is empty or unreadable
    public boolean loadGame(int slot) {
        SaveData data = Save.load(slot);
        if (data == null) return false;
        applySaveData(data);
        return true;
    }

    // SAVE: a snapshot of the game as it is right now
    private SaveData createSaveData() {
        SaveData data = new SaveData();
        data.mapName = currentMapName;
        data.playerX = player.getX();
        data.playerY = player.getY();
        data.facingDirection = player.getFacingDirection().name();
        data.level = player.getLevel();
        data.currentXP = player.getCurrentXP();
        data.xpToNextLevel = player.getXpToNextLevel();
        data.maxHP = player.getMaxHP() - player.getBonusMaxHP();
        data.currentHP = player.getCurrentHP();
        data.baseATK = player.getBaseATK() / player.getAtkModifier();
        data.baseDEF = player.getBaseDEF() / player.getDefModifier();
        data.baseSPD = player.getBaseSPD() / player.getSpdModifier();
        data.gold = PlayerData.getCurrency();
        for (NamedLocations.Place p : NamedLocations.all()) if (discovered.contains(p)) data.discoveredPlaces.add(p.name);
        data.lastPlace = lastPlace != null ? lastPlace.name : null;
        data.story = StoryState.serialize();   // STORY
        for (Item item : inventory.getAllItems()) {
            data.items.add(new SaveData.ItemSave(item.getClass().getName(), item.getItemStats().getLvl(),
                    item.getItemStats().getXp(), item.getItemStats().getDurability(), inventory.isItemEquipped(item)));
        }
        return data;
    }

    // SAVE: puts the game back the way the snapshot describes
    private void applySaveData(SaveData data) {
        PlayerData.setCurrency(data.gold);
        StoryState.load(data.story);   // STORY: before the map is rebuilt, so its story characters appear as saved

        // inventory first, so the gear's max HP bonus is in place before the player's HP is restored
        inventory.clear();
        for (SaveData.ItemSave saved : data.items) {
            Item item = createItem(saved.className);
            if (item == null) continue;
            item.getItemStats().restore(saved.level, saved.xp, saved.durability);
            inventory.addItem(item);
            if (saved.equipped) inventory.toggleItemEquip(inventory.getAllItems().size() - 1);
        }

        onMapChange(data.mapName, data.playerX, data.playerY);   // builds a fresh knight on the saved map
        try {
            player.setFacingDirection(Direction.valueOf(data.facingDirection));
        } catch (IllegalArgumentException e) {
            player.setFacingDirection(Direction.LEFT);
        }
        syncGearHealth();
        player.restoreStats(data.level, data.currentXP, data.xpToNextLevel, data.maxHP, data.currentHP,
                data.baseATK, data.baseDEF, data.baseSPD);

        discovered.clear();
        lastPlace = null;
        for (NamedLocations.Place p : NamedLocations.all()) {
            if (data.discoveredPlaces.contains(p.name)) discovered.add(p);
            if (p.name.equals(data.lastPlace)) lastPlace = p;
        }

        stepsSinceBattle = 0;
        bannerTimer = 0;
        fadeIn = 60;
        playLevelScreenState = PlayLevelScreenState.RUNNING;
    }

    // SAVE: rebuilds an item from its class name. Every item has a public constructor that takes its sprite sheet
    // (null makes it load its own). Returns null if the class no longer exists, so an old save still loads
    private static Item createItem(String className) {
        try {
            return Class.forName(className).asSubclass(Item.class)
                    .getConstructor(GameObject.SpriteSheet.class).newInstance((Object) null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            System.err.println("Skipping saved item " + className + ": " + e);
            return null;
        }
    }

    private void showSaveMessage(String message) {
        saveMessage = message;
        saveMessageTimer = 90;
    }

    private void drawSaveMessage(GraphicsHandler gh) {
        if (saveMessageTimer <= 0 || saveMessage == null) return;
        int w = hudFont.getTextWidth(saveMessage, 2, 1);
        Graphics2D g = gh.getGraphics();
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(400 - w / 2 - 12, 540, w + 24, 30);
        hudFont.draw(gh, saveMessage, 400 - w / 2, 548, 2, 1, new Color(255, 240, 200));
    }

    public PlayLevelScreenState getPlayLevelScreenState() {
        return playLevelScreenState;
    }

    public void resetLevel() {
        initialize();
    }

    public void goBackToMenu() {
        screenCoordinator.setGameState(GameState.MENU);
    }

    //Closes Attack Window
    public void closeAttackWindow(){
        playLevelScreenState = PlayLevelScreenState.RUNNING;
    }

    // SHOP: called by OpenShopScriptAction (through the map's listeners) once the shopkeeper's greeting is done
    @Override
    public void onOpenShop() {
        shopController.open(BattleWorld.fromMapName(shopWorldMapName).ordinal() + 1);
    }

    // SHOP: called by OpenBlacksmithScriptAction (through the map's listeners) once the blacksmith's greeting is done.
    // the blacksmith needs no world: an item's price comes from its own world in the registry
    @Override
    public void onOpenBlacksmith() {
        shopController.openBlacksmith();
    }

    // CHEST: called by OpenChestScriptAction (through the map's listeners). pays the gold, puts the item in the pack and tells
    // the script what to show. the chest's world comes from the name of the map it is on
    @Override
    public ChestResult onOpenChest(ChestId id) {
        return chestController.open(id, ChestWorlds.worldOf(id.mapName()), inventory);
    }

    public void closeInventory() {
        syncGearHealth();
        player.unlock();
        playLevelScreenState = PlayLevelScreenState.RUNNING;
    }

    // ---- WORLD MAP: what WorldMapScreen reads, and how it travels / closes ----
    public java.util.Set<NamedLocations.Place> getDiscoveredPlaces() {
        return java.util.Collections.unmodifiableSet(discovered);
    }

    // the place the knight is standing in right now (null on the open road between places)
    public NamedLocations.Place getCurrentPlace() {
        return placeHere();
    }

    private NamedLocations.Place placeHere() {
        return NamedLocations.find(currentMapName, player.getX(), player.getY());
    }

    public String getCurrentMapName() {
        return currentMapName;
    }

    // the world map reads a world's real tiles from this (the live map for the world the knight is in, a fresh copy otherwise)
    public Map getMapForOverview(String mapName) {
        return mapName.equals(currentMapName) ? map : buildMap(mapName);
    }

    // travels to a discovered place (same move as the JUMP buttons); anything else just closes the map
    public void travelTo(NamedLocations.Place p) {
        if (p == null || p == placeHere() || !discovered.contains(p)) {
            closeWorldMap();
            return;
        }
        moveToPlace(p);
    }

    public void closeWorldMap() {
        keyLocker.lockKey(Key.M);   // the M that closed the map must not open it again
        player.unlock();
        playLevelScreenState = PlayLevelScreenState.RUNNING;
    }

    // pushes the max HP bonus of the equipped gear into the player
    private void syncGearHealth() {
        if (player != null && inventory != null) player.setBonusMaxHP(BattleMath.bonusMaxHp(inventory));
    }

    public Inventory getInventory() { return inventory; }

    // SURVIVAL: one frame of hunger/hydration. an empty meter costs HP, but never below 1 HP (the controller decides how much)
    private void survivalTick() {
        int loss = survivalController.tick();
        if (loss == 0) return;
        float hp = player.getCurrentHP();
        player.applyDamage(hp - SurvivalController.hpAfterLoss(hp, loss));   // applyDamage: raw HP, no DEF
    }

    // SURVIVAL: called by the inventory screen (ENTER on a consumable). uses one from the pack and heals the player
    public SurvivalResult useConsumable(String itemName) {
        SurvivalResult result = survivalController.use(inventory, itemName, player.getCurrentHP(), player.getMaxHP());
        // the result's health is already clamped to the missing HP. setCurrentHP clamps to the base max HP and would cut off
        // any gear bonus, so the heal goes through applyDamage (a negative amount adds HP)
        if (result.health() > 0) player.applyDamage(-result.health());
        return result;
    }

    // SURVIVAL: the hunger and hydration bars, bottom left. hidden while the player is locked (textboxes and scripts use that area)
    private static final int SURVIVAL_BAR_W = 140, SURVIVAL_BAR_H = 14, SURVIVAL_LABEL_W = 70;
    private static final int SURVIVAL_LOW = 25;   // at or below this the bar turns red
    private static final Color SURVIVAL_BACK = new Color(20, 20, 20);
    private static final Color SURVIVAL_LOW_COLOR = new Color(200, 60, 60);
    private static final Color HUNGER_COLOR = new Color(210, 150, 60);
    private static final Color HYDRATION_COLOR = new Color(70, 140, 220);

    private void drawSurvivalBars(GraphicsHandler gh) {
        if (player.isLocked()) return;
        int x = 10;
        int hydrationY = ScreenManager.getScreenHeight() - 12 - SURVIVAL_BAR_H;
        int hungerY = hydrationY - SURVIVAL_BAR_H - 8;
        drawSurvivalBar(gh, "FOOD", survivalController.getHunger(), HUNGER_COLOR, x, hungerY);
        drawSurvivalBar(gh, "WATER", survivalController.getHydration(), HYDRATION_COLOR, x, hydrationY);
    }

    private void drawSurvivalBar(GraphicsHandler gh, String label, int value, Color color, int x, int y) {
        hudFont.draw(gh, label, x + 1, y + (SURVIVAL_BAR_H - hudFont.getCapHeight(2)) / 2 + 1, 2, 1, Color.BLACK);
        hudFont.draw(gh, label, x, y + (SURVIVAL_BAR_H - hudFont.getCapHeight(2)) / 2, 2, 1, Color.WHITE);
        int barX = x + SURVIVAL_LABEL_W;
        gh.drawFilledRectangleWithBorder(barX, y, SURVIVAL_BAR_W, SURVIVAL_BAR_H, SURVIVAL_BACK, Color.BLACK, 2);
        int fill = (SURVIVAL_BAR_W - 4) * value / 100;
        if (fill > 0) gh.drawFilledRectangle(barX + 2, y + 2, fill, SURVIVAL_BAR_H - 4, value <= SURVIVAL_LOW ? SURVIVAL_LOW_COLOR : color);
    }

    // This enum represents the different states this screen can be in
    public enum PlayLevelScreenState {
        RUNNING, LEVEL_COMPLETED, ATTACK_WINDOW,INVENTORY, BATTLE, ENCOUNTER, GAME_OVER, WORLD_MAP
    }
}