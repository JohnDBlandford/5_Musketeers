package Screens;

import Engine.*;
import Game.GameState;
import Game.ScreenCoordinator;
import Item.ItemTestFactory;
import Game.PlayerData;
import Level.*;
import Maps.BlankMap;
import Maps.TestMap;
import Maps.World1Map;
import Maps.World2Map;
import NPCs.Enemy;
import Players.Knight;
import BattleSystem.BattleListener;
import Encounters.EnemyFactory;
import Locations.NamedLocations;
import BattleSystem.BattleResult;
import BattleSystem.BattleWorld;
import Screens.Battle.EncounterScreen;
import Screens.Battle.PlayBattleScreen;
import SpriteFont.MonoSpriteFont;
import SpriteFont.SpriteFont;
import Utils.Direction;
import java.awt.Color;
import java.awt.Graphics2D;
import inventory.Inventory;

// This class is for when the RPG game is actually being played
// BATTLE CHANGES (search for "BATTLE"): the battle fields, the BATTLE case in update()/draw(), currentMapName tracking in
// initialize()/onMapChange(), startBattle()/onBattleEnd(), the waypoint list (M) and place discovery.
public class PlayLevelScreen extends Screen implements GameListener, BattleListener {
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
    private Inventory inventory;

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
    private NamedLocations.Place lastPlace;     // the last named location the player was in (null until he enters one)
    private int bannerTimer;                    // frames left of the "place name" banner
    private boolean bannerNew;                 // the banner is for a newly discovered place
    private final java.util.Set<NamedLocations.Place> discovered = new java.util.HashSet<>();
    private static final int MAX_WAYPOINTS = 9;   // buttons shown at once (per world)
    private final MonoSpriteFont hudFont = new MonoSpriteFont();
    private int fadeIn;                         // frames left of the black fade-in after a respawn
    private final MonoSpriteFont bannerFont = new MonoSpriteFont();

    public PlayLevelScreen(ScreenCoordinator screenCoordinator) {
        this.screenCoordinator = screenCoordinator;
    }

    public void initialize() {
        // setup state
        flagManager = new FlagManager();
        flagManager.addFlag("hasLostBall", false);
        flagManager.addFlag("hasTalkedToWalrus", false);
        flagManager.addFlag("hasTalkedToDinosaur", false);
        flagManager.addFlag("hasFoundBall", false);
        inventory = new Inventory();

        // define/setup map
        map = new World1Map();
        currentMapName = "world1";   // BATTLE
        map.setFlagManager(flagManager);

        // setup player
        player = new Knight(map.getPlayerStartPosition().x, map.getPlayerStartPosition().y);
        player.setMap(map);
        playLevelScreenState = PlayLevelScreenState.RUNNING;
        player.setFacingDirection(Direction.LEFT);
        lastPlace = null;
        discovered.clear();
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

        // preloads all scripts ahead of time rather than loading them dynamically
        // both are supported, however preloading is recommended
        map.preloadScripts();

        winScreen = new WinScreen(this);
        attackWindow = new AttackWindow(this);

        // initialize currency counter display
        coinCountDisplay = new SpriteFont("Gold: " + PlayerData.getCurrency(), 10, 30, "Arial", 24, Color.YELLOW);
        coinCountDisplay.setOutlineColor(Color.BLACK);
        coinCountDisplay.setOutlineThickness(2);
        inventoryScreen = new InventoryScreen(this);
        inventory.addItem(ItemTestFactory.createRustedShortSword(null));
        inventory.addItem(ItemTestFactory.createRustedShortSword(null));   // duplicate, to test the identity swap
        inventory.addItem(ItemTestFactory.createWraithboneSaber(null));    // legendary, to test the color
        inventory.addItem(ItemTestFactory.createCrackedHuntingBow(null));
        inventory.addItem(ItemTestFactory.createPatchedWoodShield(null));
        inventory.addItem(ItemTestFactory.createFamineEdgeVestmentsArmor(null));
    }

    public void update() {
        if (inventory != null) inventory.removeBrokenItem();
        if (Keyboard.isKeyUp(Key.I)) keyLocker.unlockKey(Key.I);
        MouseInput.update();
        java.awt.Point click = MouseInput.pollClick();   // a mouse click this frame, if any
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
                // waypoints: click a JUMP button on the right of the screen to travel to that place
                if (click != null && !player.isLocked() && fadeIn == 0) {
                    java.util.List<NamedLocations.Place> list = waypointList();
                    for (int n = 0; n < list.size(); n++) {
                        if (waypointButton(n).contains(click) && list.get(n) != lastPlace) {
                            moveToPlace(list.get(n));
                            break;
                        }
                    }
                }
                if (playLevelScreenState != PlayLevelScreenState.RUNNING) break;
                player.update();
                map.update(player);
                trackPlace();
                checkRandomEncounter();
                if (fadeIn > 0) fadeIn--;
                if (bannerTimer > 0) bannerTimer--;
                // Opens attack window at random
                if(Keyboard.isKeyDown(Key.W) && !keyLocker.isKeyLocked(Key.W)){
                    keyLocker.lockKey(Key.W);
                    int randomNum = (int) Math.round(Math.random() * 100%5);
                    if(randomNum == 0){
                        playLevelScreenState = PlayLevelScreenState.ATTACK_WINDOW;
                    }
                }
                if(Keyboard.isKeyUp(Key.W)){
                    keyLocker.unlockKey(Key.W);
                }
                coinCountDisplay.setText("Gold: " + PlayerData.getCurrency());
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
                    battleScreen = new PlayBattleScreen(player, inventory, pendingEnemy, pendingLevel, pendingWorld, this);
                    battleScreen.setBoss(pendingBoss);
                    battleScreen.initialize();
                    playLevelScreenState = PlayLevelScreenState.BATTLE;
                }
                break;

            // GAME_OVER: after ENTER the knight is put back at the last named location
            case GAME_OVER:
                gameOverScreen.update();
                if (gameOverScreen.isDone()) respawn();
                break;
        }
    }

    // BATTLE: starts a fight against the given enemy. The background is picked from the map the player is standing in.
    // Call this from wherever an encounter should happen. Pass boss = true for bosses (no running away, 2 heals).
    public void startBattle(Enemy enemy, int enemyLevel, boolean boss) {
        startBattle(enemy, enemyLevel, boss, BattleWorld.fromMapName(currentMapName));
    }

    // same, but with the battle background chosen by the caller
    public void startBattle(Enemy enemy, int enemyLevel, boolean boss, BattleWorld world) {
        player.lock();
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
        }
    }

    // moves the knight to a named place's fixed tile (switching maps if it is in another world; the same knight goes along,
    // so level, XP, HP and stats are kept) and fades the world back in
    private void moveToPlace(NamedLocations.Place p) {
        if (p == null) {
            player.setLocation(map.getPlayerStartPosition().x, map.getPlayerStartPosition().y);
        } else if (!p.map.equals(currentMapName)) {
            float[] sp = NamedLocations.spawnPixels(p, createMap(p.map));
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
        player.setCurrentHP(player.getMaxHP());
        moveToPlace(lastPlace);
    }

    // ---- random encounters: every full tile the knight walks outside a SAFE place may start a fight ----
    private static final int GRACE_STEPS = 6;           // no fight until this many steps after the last one
    private static final double STEP_CHANCE = 0.07;     // chance per step after the grace period
    private float lastX, lastY, stepDist;
    private int stepsSinceBattle;
    private final java.util.Random encRng = new java.util.Random();

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
        EnemyFactory.Encounter enc = EnemyFactory.randomEncounter(currentMapName, encRng);
        if (enc == null) return;   // this world has no random encounters yet
        stepsSinceBattle = 0;
        startBattle(enc.enemy, 1, enc.boss);
    }

    // BATTLE: the battle calls this when it has finished fading out
    @Override
    public void onBattleEnd(BattleResult result) {
        player.unlock();
        playLevelScreenState = PlayLevelScreenState.RUNNING;
        if (result == BattleResult.DEFEAT) {
            // the knight fell: show the game-over screen, which then sends him back to the last named location
            player.lock();
            gameOverScreen = new GameOverScreen(lastPlace != null ? lastPlace.name : "the start");
            gameOverScreen.initialize();
            playLevelScreenState = PlayLevelScreenState.GAME_OVER;
        }
        // VICTORY: XP and gold were already given by the battle.  FLED: nothing to do.
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
        map = createMap(mapName);
        currentMapName = mapName;   // BATTLE
        map.setFlagManager(flagManager);
        // keep the SAME knight (level, XP, HP, stats and everything else carry over between worlds); just move him
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
        map.preloadScripts();
        trackPlace();
    }

    // This method creates a new map based on the provided map name. It returns a
    // new instance of the appropriate map class.
    private Map createMap(String mapName) {
        switch (mapName) {
            case "blank":
                return new BlankMap();
            case "test":
                return new TestMap();
            case "world1":
                return new World1Map();
            case "world2_map":
                return new World2Map();
            default:
                return new TestMap();
        }
    }

    public void draw(GraphicsHandler graphicsHandler) {
        // based on screen state, draw appropriate graphics
        switch (playLevelScreenState) {
            case RUNNING:
                map.draw(player, graphicsHandler);
                coinCountDisplay.draw(graphicsHandler);
                drawPlaceBanner(graphicsHandler);
                drawWaypointButtons(graphicsHandler);
                if (fadeIn > 0) {
                    graphicsHandler.getGraphics().setColor(new Color(0, 0, 0, Math.min(255, fadeIn * 255 / 60)));
                    graphicsHandler.getGraphics().fillRect(0, 0, 800, 605);
                }
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
    private java.util.List<NamedLocations.Place> waypointList() {
        java.util.List<NamedLocations.Place> out = new java.util.ArrayList<>();
        for (NamedLocations.Place p : NamedLocations.all())
            if (p.map.equals(currentMapName) && discovered.contains(p) && out.size() < MAX_WAYPOINTS) out.add(p);
        return out;
    }

    // where the i-th JUMP button is on the screen
    private java.awt.Rectangle waypointButton(int i) {
        int size = 34, gap = 8;
        return new java.awt.Rectangle(800 - size - 14, 70 + i * (size + gap), size, size);
    }

    // the JUMP buttons down the right side of the screen: click one to travel there.
    // A new button appears the first time the knight discovers a place; the one for the current place is lit green.
    private void drawWaypointButtons(GraphicsHandler gh) {
        java.util.List<NamedLocations.Place> list = waypointList();
        if (list.size() < 2) return;   // nothing to jump to yet
        Graphics2D g = gh.getGraphics();
        java.awt.Rectangle first = waypointButton(0);
        hudFont.draw(gh, "JUMP", first.x + first.width / 2 - hudFont.getTextWidth("JUMP", 2, 1) / 2, first.y - 22, 2, 1, new Color(255, 240, 200));
        for (int i = 0; i < list.size(); i++) {
            NamedLocations.Place p = list.get(i);
            java.awt.Rectangle r = waypointButton(i);
            boolean here = p == lastPlace;
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
        int w = bannerFont.getTextWidth(lastPlace.name, 3, 2);
        Graphics2D g = gh.getGraphics();
        g.setColor(new Color(0, 0, 0, a * 140 / 255));
        g.fillRect(400 - w / 2 - 24, 56, w + 48, bannerNew ? 64 : 44);
        bannerFont.draw(gh, lastPlace.name, 400 - w / 2, 68, 3, 2, new Color(255, 235, 190, a));
        if (bannerNew) {
            String tag = "NEW WAYPOINT UNLOCKED";
            bannerFont.draw(gh, tag, 400 - bannerFont.getTextWidth(tag, 1, 1) / 2, 100, 1, 1, new Color(150, 220, 150, a));
        }
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

    public void closeInventory() {
        player.unlock();
        playLevelScreenState = PlayLevelScreenState.RUNNING;
    }

    public Inventory getInventory() { return inventory; }

    // This enum represents the different states this screen can be in
    private enum PlayLevelScreenState {
        RUNNING, LEVEL_COMPLETED, ATTACK_WINDOW,INVENTORY, BATTLE, ENCOUNTER, GAME_OVER
    }
}