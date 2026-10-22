package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;
import Engine.IScreen;

// Base class for every pop-up menu in the battle (the main options, the ability window, the ability info, the run confirm).
// It is NOT a Screen, it only implements IScreen, and PlayBattleScreen owns and draws all of them.
//
// The rule from the design: when the player interacts with an option, the player's SPRITE CHANGES and the MOVE IS PERFORMED.
// interact(index) does exactly that, in that order, so subclasses only say which pose to use and what the move does.
public abstract class OptionScreen implements IScreen {
    protected final PlayBattleScreen battle;
    protected boolean open;
    protected int cursor;

    protected OptionScreen(PlayBattleScreen battle) {
        this.battle = battle;
    }

    // how many options the menu has
    protected abstract int optionCount();

    // which pose the player's sprite switches to when this option is chosen
    protected abstract BattleSprites.BattleSprite.Pose poseFor(int index);

    // the move itself
    protected abstract void perform(int index);

    // sprite changes + move is performed
    protected final void interact(int index) {
        if (index < 0 || index >= optionCount()) return;
        battle.getPlayerSprite().setPose(poseFor(index));
        perform(index);
    }

    public void open() {
        open = true;
        cursor = 0;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public int getCursor() {
        return cursor;
    }

    @Override
    public void initialize() {
        open = false;
        cursor = 0;
    }

    @Override
    public abstract void update();

    @Override
    public abstract void draw(GraphicsHandler graphicsHandler);

    // arrow keys / WASD move the cursor around a grid with the given number of columns
    protected final void navigate(int cols) {
        int count = optionCount();
        if (BattleInput.left() && cursor % cols > 0) cursor--;
        if (BattleInput.right() && cursor % cols < cols - 1 && cursor + 1 < count) cursor++;
        if (BattleInput.up() && cursor - cols >= 0) cursor -= cols;
        if (BattleInput.down() && cursor + cols < count) cursor += cols;
    }
}