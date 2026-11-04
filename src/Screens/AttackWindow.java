package Screens;

import Engine.*;
import SpriteFont.SpriteFont;
import Engine.Keyboard;
import Engine.Key;

import java.awt.*;

public class AttackWindow extends Screen {
    protected SpriteFont testMessage;
    protected SpriteFont god;
    protected SpriteFont closeWindowText;
    protected KeyLocker keyLocker = new KeyLocker();
    protected PlayLevelScreen playLevelScreen;


    public AttackWindow(PlayLevelScreen playLevelScreen) {
        this.playLevelScreen = playLevelScreen;
        initialize();
    }



    public void initialize() {
        testMessage =  new SpriteFont("You found the window I was TOLD I MUST KEEP", 300, 200, "Arial", 20, Color.white);
        god =  new SpriteFont("PEER PRESSURE", 300, 240, "Arial", 20, Color.white);
        closeWindowText =  new SpriteFont("Press ENTER to close window", 300, 280, "Arial", 20, Color.white);

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

        graphicsHandler.drawFilledRectangle(300,150,400,300,Color.RED);
        testMessage.draw(graphicsHandler);
        god.draw(graphicsHandler);
        closeWindowText.draw(graphicsHandler);


    }



}
