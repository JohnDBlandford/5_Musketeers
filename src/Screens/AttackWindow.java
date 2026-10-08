package Screens;

import Engine.*;
import SpriteFont.SpriteFont;
import Engine.Keyboard;
import Engine.Key;
import Screens.PlayLevelScreen;

import java.awt.*;

public class AttackWindow extends Screen {
    protected SpriteFont testMessage;
    protected SpriteFont closeWindowText;
    protected KeyLocker keyLocker = new KeyLocker();
    protected PlayLevelScreen playLevelScreen;


    public AttackWindow(PlayLevelScreen playLevelScreen) {
        this.playLevelScreen = playLevelScreen;
        initialize();
    }



    public void initialize() {
        testMessage =  new SpriteFont("Place Holder: UNDER ATTACK ", 300, 200, "Arial", 20, Color.white);
        closeWindowText =  new SpriteFont("Press ENTER to close window", 300, 250, "Arial", 20, Color.white);

    }

    public void update() {


        if(Keyboard.isKeyDown(Key.ENTER) && !keyLocker.isKeyLocked(Key.ENTER)) {
            keyLocker.lockKey(Key.ENTER);
            //
            playLevelScreen.closeAttackWindow();
        }
        if(Keyboard.isKeyUp(Key.ENTER)){
            keyLocker.unlockKey(Key.ENTER);
        }


    }

    public void draw(GraphicsHandler graphicsHandler) {

        graphicsHandler.drawFilledRectangle(300,150,300,300,Color.RED);
        testMessage.draw(graphicsHandler);
        closeWindowText.draw(graphicsHandler);


    }



}
