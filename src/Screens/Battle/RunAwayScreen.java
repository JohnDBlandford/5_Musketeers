package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;

// "RUN AWAY?  YES / NO". YES switches the player to the running pose and attempts to flee (the Gen 1 Pokemon run formula, see BattleMath.runEscapes).
public final class RunAwayScreen extends OptionScreen {
    public RunAwayScreen(PlayBattleScreen battle) {
        super(battle);
    }

    @Override
    protected int optionCount() { return 1; }   // only YES is a move; NO just backs out

    @Override
    protected BattleSprites.BattleSprite.Pose poseFor(int index) { return BattleSprites.BattleSprite.Pose.RUN; }

    @Override
    protected void perform(int index) {
        close();
        battle.attemptRun();
    }

    @Override
    public void update() {
        if (!open) return;
        if (BattleInput.left() || BattleInput.up()) cursor = 0;
        if (BattleInput.right() || BattleInput.down()) cursor = 1;
        if (BattleInput.back()) { close(); battle.backToCommands(); return; }
        if (BattleInput.confirm()) {
            if (cursor == 0) interact(0);
            else { close(); battle.backToCommands(); }
        }
    }

    @Override
    public void draw(GraphicsHandler gh) {
        if (!open) return;
        int x = BattleUI.btnX(), y = BattleUI.barTop(), w = BattleUI.btnW(), h = BattleUI.barHeight();
        BattleUI.box(gh, x, y, w, h);
        BattleUI.text(gh, "RUN AWAY?", x + 20, y + 18, 2, BattleUI.INK);
        BattleUI.text(gh, battle.isBoss() ? "THIS ENEMY WILL NOT LET YOU GO." : "FASTER FIGHTERS ESCAPE MORE EASILY.", x + 20, y + 46, 1, BattleUI.INK_SOFT);
        int bw = (w - 60) / 2, by = y + 66, bh = 52;
        String[] labels = {"YES", "NO"};
        java.awt.Color[] colors = {BattleUI.BTN_RUN, BattleUI.BTN_MELEE};
        for (int i = 0; i < 2; i++) {
            int bx = x + 20 + i * (bw + 20);
            BattleUI.button(gh, bx, by, bw, bh, colors[i], i == cursor, false);
            BattleUI.textCenter(gh, labels[i], bx + bw / 2, by + 19, 2, BattleUI.WHITE);
        }
    }
}