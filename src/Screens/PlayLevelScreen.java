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
import Players.Knight;
import SpriteFont.SpriteFont;
import Utils.Direction;
import java.awt.Color;
import inventory.Inventory;

// This class is for when the RPG game is actually being played
public class PlayLevelScreen extends Screen implements GameListener {
    protected ScreenCoordinator screenCoordinator;
    protected Map map;
    protected Player player;
    protected PlayLevelScreenState playLevelScreenState;
    protected WinScreen winScreen;
    protected FlagManager flagManager;
    protected SpriteFont coinCountDisplay;
    private KeyLocker keyLocker = new KeyLocker();
    private InventoryScreen inventoryScreen;
    private Inventory inventory;

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
        map.setFlagManager(flagManager);

        // setup player
        player = new Knight(map.getPlayerStartPosition().x, map.getPlayerStartPosition().y);
        player.setMap(map);
        playLevelScreenState = PlayLevelScreenState.RUNNING;
        player.setFacingDirection(Direction.LEFT);

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
        if (Keyboard.isKeyUp(Key.I)) keyLocker.unlockKey(Key.I);
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
                player.update();
                map.update(player);
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
        }
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
        map.setFlagManager(flagManager);
        player = new Knight(playerX, playerY);
        player.setMap(map);
        player.setFacingDirection(Direction.LEFT);
        map.setPlayer(player);
        map.getTextbox().setInteractKey(player.getInteractKey());
        map.addListener(this);
        map.preloadScripts();
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
                break;
            case LEVEL_COMPLETED:
                winScreen.draw(graphicsHandler);
                break;
            case INVENTORY:
                inventoryScreen.draw(graphicsHandler);
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

    public void closeInventory() {
        player.unlock();
        playLevelScreenState = PlayLevelScreenState.RUNNING;
    }

    public Inventory getInventory() { return inventory; }

    // This enum represents the different states this screen can be in
    private enum PlayLevelScreenState {
        RUNNING, LEVEL_COMPLETED, INVENTORY
    }
}