package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;

import java.util.List;

// Shows the name, type, power and description of the ability you pressed Y on.
// ENTER from here uses the move, Y or B closes it and goes back to the ability window.
public class AbilityInfoScreen extends OptionScreen {
    private Ability ability;

    public AbilityInfoScreen(PlayBattleScreen battle) {
        super(battle);
    }

    public void open(Ability ability) {
        super.open();
        this.ability = ability;
    }

    @Override
    protected int optionCount() { return ability == null ? 0 : 1; }

    @Override
    protected BattleSprites.BattleSprite.Pose poseFor(int index) {
        return ability.getCategory() == MoveCategory.SHIELD ? BattleSprites.BattleSprite.Pose.GUARD
                : BattleSprites.BattleSprite.Pose.ATTACK;
    }

    @Override
    protected void perform(int index) {
        close();
        battle.useAbility(ability);
    }

    @Override
    public void update() {
        if (!open) return;
        if (BattleInput.info() || BattleInput.back()) { close(); battle.closeAbilityInfo(); return; }
        if (BattleInput.confirm()) interact(0);
    }

    @Override
    public void draw(GraphicsHandler gh) {
        if (!open || ability == null) return;
        int x = BattleUI.btnX(), y = BattleUI.barTop(), w = BattleUI.btnW(), h = BattleUI.barHeight();
        BattleUI.box(gh, x, y, w, h);
        BattleUI.text(gh, ability.getName(), x + 20, y + 18, 2, AbilityScreen.colorFor(ability.getCategory()).darker());
        BattleUI.text(gh, "TYPE " + ability.getCategory().getLabel() + "   POWER " + ability.getPowerText() + "   ACC " + ability.getAccuracyText(), x + 20, y + 44, 1, BattleUI.INK_SOFT);
        List<String> lines = BattleUI.wrap(ability.getDescription(), (w - 40) / 12);
        for (int i = 0; i < lines.size() && i < 3; i++) BattleUI.text(gh, lines.get(i), x + 20, y + 62 + i * 22, 2, BattleUI.INK);
        BattleUI.text(gh, "ENTER USE   Y OR B BACK", x + 20, y + h - 22, 1, BattleUI.INK_SOFT);
    }
}