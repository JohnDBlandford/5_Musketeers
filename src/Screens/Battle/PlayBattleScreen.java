package Screens.Battle;

import Engine.GraphicsHandler;
import Engine.Screen;

public class PlayBattleScreen extends Screen {
    private final BattleBackgroundScreen battleBackgroundScreen;
    private final TurnHistoryScreen turnHistoryScreen;
    private final InventoryDisplayScreen inventoryDisplayScreen;
    private final OptionScreen optionScreen;
    private final AbilityScreen abilityScreen;
    private final AbilityInfoScreen abilityInfoScreen;
    private final RunAwayScreen runAwayScreen;

    public PlayBattleScreen() {
        battleBackgroundScreen = new BattleBackgroundScreen();
        turnHistoryScreen = new TurnHistoryScreen();
        inventoryDisplayScreen = new InventoryDisplayScreen();
        optionScreen = new OptionScreen() {
            @Override
            public void initialize() {

            }

            @Override
            public void update() {

            }

            @Override
            public void draw(GraphicsHandler graphicsHandler) {

            }
        };
        abilityScreen = new AbilityScreen();
        abilityInfoScreen = new AbilityInfoScreen();
        runAwayScreen = new RunAwayScreen();
    }

    @Override
    public void initialize() {

    }

    @Override
    public void update() {

    }

    @Override
    // order of init matters here
    public void draw(GraphicsHandler graphicsHandler) {
        battleBackgroundScreen.draw(graphicsHandler);
        turnHistoryScreen.draw(graphicsHandler);
        inventoryDisplayScreen.draw(graphicsHandler);
        optionScreen.draw(graphicsHandler);
        abilityScreen.draw(graphicsHandler);
        abilityInfoScreen.draw(graphicsHandler);
        runAwayScreen.draw(graphicsHandler);
    }
}
