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
import java.awt.*;
import inventory.Inventory;

// This class is for when the RPG game is actually being played
public class PlayLevelScreen extends Screen implements GameListener {
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
    //XP DISPLAY TESTING
    private SpriteFont xpDisplay;
    private int xp;
    Color xpColor;

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
        flagManager.addFlag("sawIntro", false);

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

                //XP TESTING
                if(Keyboard.isKeyDown(Key.X) && !keyLocker.isKeyLocked(Key.X)) {
                    keyLocker.lockKey(Key.X);
                    xp = xp + 1;
                }
                if(Keyboard.isKeyUp(Key.X)){
                    keyLocker.unlockKey(Key.X);
                }
                xpDisplay = new SpriteFont("XP " + xp ,10,8,"Arial",20,Color.WHITE);
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
                //DISPLAY XP
                xpDisplay.draw(graphicsHandler);
                graphicsHandler.drawFilledRectangle(77, 8, 307, 25,new Color(0,0,0));
                graphicsHandler.drawFilledRectangle(80,10,30,20,new Color(204,255,204));
                graphicsHandler.drawFilledRectangle(110,10,30,20,new Color(188, 246, 188));
                graphicsHandler.drawFilledRectangle(140,10,30,20,new Color(165, 243, 165));
                graphicsHandler.drawFilledRectangle(170,10,30,20,new Color(141, 244, 141));
                graphicsHandler.drawFilledRectangle(200,10,30,20,new Color(113, 244, 113));
                graphicsHandler.drawFilledRectangle(230,10,30,20,new Color(93, 246, 93));
                graphicsHandler.drawFilledRectangle(260,10,30,20,new Color(66, 243, 66));
                graphicsHandler.drawFilledRectangle(290,10,30,20,new Color(49, 243, 49, 216));
                graphicsHandler.drawFilledRectangle(320,10,30,20,new Color(24, 243, 24, 163));
                graphicsHandler.drawFilledRectangle(350,10,30,20,new Color(0, 255, 0, 115));
                graphicsHandler.drawFilledRectangle(80 + (xp * 3), 10, 300 - (xp * 3), 20, Color.LIGHT_GRAY);
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
        RUNNING, LEVEL_COMPLETED, ATTACK_WINDOW,INVENTORY
    }
}