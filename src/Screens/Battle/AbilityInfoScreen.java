package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Shows the name, type, power and description of the ability you pressed Y on.
// ENTER from here uses the move, Y or B closes it and goes back to the ability window.
//
// BALANCE CHANGES (search for "BALANCE:"): this window now holds EVERY detail of a move, because the move buttons only show
// PWR and PP. Added here:
//   - PP left on the stats line
//   - the move's rules in red under it: priority, can't-repeat, recharge, BRACE's damage cut, SHIELD THROW's shield bonus
//     (any new rule should be added in rules() below, not on the button)
public final class AbilityInfoScreen extends OptionScreen {
    private Ability ability;

    private static final Color RULE = new Color(170, 40, 40);

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

    // BALANCE: the special rules of a move, one short line each (shown in red). Add new move rules here.
    private List<String> rules(Ability a) {
        List<String> out = new ArrayList<>();
        if (PlayBattleScreen.isGuardMove(a)) out.add("GOES FIRST. CAN'T BE USED TWICE IN A ROW.");
        if (PlayBattleScreen.isBrace(a)) out.add("CUTS DAMAGE TAKEN BY " + Math.round(Balance.BRACE_REDUCTION * 100) + "% FOR "
                + Balance.BRACE_TURNS + " ENEMY TURNS.");
        else if (PlayBattleScreen.isGuardMove(a)) out.add("BLOCKS THE NEXT ENEMY HIT COMPLETELY.");
        if (Balance.recharges(a)) out.add("RECHARGE: NO MELEE OR RANGED ON YOUR NEXT TURN.");
        if (a.getEffect() == Ability.Effect.THROW)
            out.add("POWER INCLUDES YOUR SHIELD'S " + Math.round(BattleMath.blockAmount(battle.getInventory()) * 100)
                    + "% BLOCK. USABLE WHILE RECHARGING.");
        if (a == Balance.STRUGGLE) out.add("YOU TAKE " + Math.round(Balance.STRUGGLE_RECOIL * 100) + "% OF THE DAMAGE AS RECOIL.");
        return out;
    }

    @Override
    public void draw(GraphicsHandler gh) {
        if (!open || ability == null) return;
        int x = BattleUI.btnX(), y = BattleUI.barTop(), w = BattleUI.btnW(), h = BattleUI.barHeight();
        BattleUI.box(gh, x, y, w, h);
        BattleUI.text(gh, ability.getName(), x + 20, y + 18, 2, AbilityScreen.colorFor(ability.getCategory()).darker());
        // BALANCE: PP added; SHIELD THROW's power includes the shield
        String power = AbilityScreen.powerText(ability, BattleMath.blockAmount(battle.getInventory()));
        int left = battle.ppLeft(ability), max = battle.ppMax(ability);
        String pp = left < 0 ? "PP --" : "PP " + left + "/" + max;
        BattleUI.text(gh, "TYPE " + ability.getCategory().getLabel() + "  POWER " + power + "  ACC " + ability.getAccuracyText() + "  " + pp,
                x + 20, y + 40, 1, BattleUI.INK_SOFT);

        // BALANCE: the move's rules (small red lines), then the description in whatever room is left
        int ty = y + 54, smallChars = (w - 40) / 6;
        for (String rule : rules(ability))
            for (String line : BattleUI.wrap(rule, smallChars)) {
                BattleUI.text(gh, line, x + 20, ty, 1, RULE);
                ty += 11;
            }
        int footer = y + h - 22;
        int scale = ty > y + 60 ? 1 : 2;   // with rules shown the description drops to the small size so it still fits
        int lineH = scale == 2 ? 22 : 11;
        List<String> lines = BattleUI.wrap(ability.getDescription(), (w - 40) / (6 * scale));
        ty += 4;
        for (int i = 0; i < lines.size() && ty + BattleUI.font().getCapHeight(scale) < footer - 4; i++, ty += lineH)
            BattleUI.text(gh, lines.get(i), x + 20, ty, scale, BattleUI.INK);
        BattleUI.text(gh, "ENTER USE   Y OR B BACK", x + 20, footer, 1, BattleUI.INK_SOFT);
    }
}