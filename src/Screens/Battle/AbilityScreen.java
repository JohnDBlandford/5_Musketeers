package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;
import inventory.Inventory;

import java.awt.*;

// The 2x2 window of abilities for MELEE, SHIELD or RANGED, numbered 1-4 (press 1-4 to jump to a slot).
// It takes the place of the main buttons. ENTER uses the hovered ability, Y shows its name / power / description, B goes back.
public class AbilityScreen extends OptionScreen {
    private MoveCategory category = MoveCategory.MELEE;
    private Ability[] slots = new Ability[4];
    private String itemName = "";

    public AbilityScreen(PlayBattleScreen battle) {
        super(battle);
    }

    public void open(MoveCategory category, Inventory inventory) {
        super.open();
        this.category = category;
        this.slots = AbilityLibrary.slotsFor(category, inventory.getEquippedItem(category.getSlot()));
        this.itemName = inventory.getEquippedItem(category.getSlot()) == null ? "Nothing equipped" : BattleUI.prettyName(inventory.getEquippedItem(category.getSlot()).getItemName());
    }

    // coming back from the info window: keep the cursor where it was
    void resume() { open = true; }

    public Ability getHovered() {
        return slots[cursor];
    }

    MoveCategory getCategory() { return category; }
    String getItemName() { return itemName; }

    @Override
    protected int optionCount() { return 4; }

    @Override
    protected BattleSprite.Pose poseFor(int index) {
        return category == MoveCategory.SHIELD ? BattleSprite.Pose.GUARD : BattleSprite.Pose.ATTACK;
    }

    @Override
    protected void perform(int index) {
        close();
        battle.useAbility(slots[index]);
    }

    // use a slot (ignores empty slots)
    void choose(int index) {
        if (index >= 0 && index < 4 && slots[index] != null) interact(index);
    }

    @Override
    public void update() {
        if (!open) return;
        navigate(2);
        int n = BattleInput.number();
        if (n >= 0) cursor = n;
        if (BattleInput.back()) { close(); battle.backToCommands(); return; }
        if (BattleInput.info() && getHovered() != null) { battle.openAbilityInfo(getHovered()); return; }
        if (BattleInput.confirm()) choose(cursor);
    }

    static Color colorFor(MoveCategory c) {
        switch (c) {
            case MELEE: return BattleUI.BTN_MELEE;
            case SHIELD: return BattleUI.BTN_SHIELD;
            default: return BattleUI.BTN_RANGED;
        }
    }

    @Override
    public void draw(GraphicsHandler gh) {
        if (!open) return;
        int x = BattleUI.btnX(), y = BattleUI.barTop() + 2, w = BattleUI.btnW();
        int gap = 8, cellW = (w - gap) / 2, cellH = 70;
        for (int i = 0; i < 4; i++) {
            int cx = x + (i % 2) * (cellW + gap), cy = y + (i / 2) * (cellH + gap);
            Ability a = slots[i];
            boolean hover = i == cursor;
            BattleUI.button(gh, cx, cy, cellW, cellH, colorFor(category), hover, a == null);
            // number badge
            gh.drawFilledRectangle(cx + 12, cy + 12, 22, 22, hover ? BattleUI.GOLD : new Color(255, 255, 255, 220));
            BattleUI.textCenter(gh, String.valueOf(i + 1), cx + 23, cy + 16, 2, BattleUI.INK);

            // long names (like BLOODFANG STRIKE) wrap onto two lines instead of running out of the button
            String name = a == null ? "---" : a.getName();
            int avail = cellW - 46 - 10, lines = 1;
            int tx = cx + 42;
            if (BattleUI.textW(name, 2) <= avail || name.indexOf(' ') < 0) {
                BattleUI.shadowText(gh, name, tx, cy + 12, 2, BattleUI.WHITE);
            } else {
                int cut = name.indexOf(' ');
                BattleUI.shadowText(gh, name.substring(0, cut), tx, cy + 12, 2, BattleUI.WHITE);
                BattleUI.shadowText(gh, name.substring(cut + 1), tx, cy + 29, 2, BattleUI.WHITE);
                lines = 2;
            }
            int subY = cy + (lines == 2 ? 52 : 36);
            if (a != null) BattleUI.shadowText(gh, "POWER " + a.getPowerText() + "  ACC " + a.getAccuracyText(), tx, subY, 1, BattleUI.WHITE);
            else BattleUI.shadowText(gh, "NOT ON THIS ITEM", tx, subY, 1, BattleUI.WHITE);
        }
    }
}