package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;
import inventory.Inventory;

import java.awt.*;

// The 2x2 window of abilities for MELEE, SHIELD or RANGED, numbered 1-4 (press 1-4 to jump to a slot).
// It takes the place of the main buttons. ENTER uses the hovered ability, Y shows its name / power / description, B goes back.
//
// BALANCE CHANGES (search for "BALANCE:"):
//   - RULE: a button only ever shows "PWR" and "PP" (power, uses left). Everything else about a move (priority, recharge,
//     BRACE's damage cut, accuracy, the description) is in the Y info window (AbilityInfoScreen), for every move.
//   - each button shows the move's PP left ("PP 3/5", or "PP --" for unlimited) instead of its accuracy
//   - a move at 0 PP is drawn greyed out and refused with "NO PP LEFT FOR ...!" in the text box
//   - BULWARK STANCE / BRACE are greyed out right after being used (can't be used twice in a row)
//   - SHIELD THROW shows its REAL power with the equipped shield (100% x (1 + block amount), e.g. "PWR 155%")
//   - GLITCH: a move torn out by the glitch's SEVERANCE is drawn as a glitched box (scrambled name, no numbers) and refused
//   - a move the knight hasn't learned yet (Balance.MOVE_UNLOCK_LEVELS) is greyed out as "LOCKED - LV 10" and refused
public final class AbilityScreen extends OptionScreen {
    private MoveCategory category = MoveCategory.MELEE;
    private Ability[] slots = new Ability[4];
    private String itemName = "";
    private double shieldBlock;   // BALANCE: the equipped shield's block amount (SHIELD THROW scales with it)

    public AbilityScreen(PlayBattleScreen battle) {
        super(battle);
    }

    public void open(MoveCategory category, Inventory inventory) {
        super.open();
        this.category = category;
        this.slots = AbilityLibrary.slotsFor(category, inventory.getEquippedItem(category.getSlot()));
        this.shieldBlock = BattleMath.blockAmount(inventory);   // BALANCE
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
    protected BattleSprites.BattleSprite.Pose poseFor(int index) {
        return category == MoveCategory.SHIELD ? BattleSprites.BattleSprite.Pose.GUARD : BattleSprites.BattleSprite.Pose.ATTACK;
    }

    @Override
    protected void perform(int index) {
        close();
        battle.useAbility(slots[index]);
    }

    // BALANCE: true when the knight hasn't reached this slot's unlock level yet
    private boolean locked(int index) {
        return !battle.slotUnlocked(index);   // (GLITCH TEST MODE unlocks everything)
    }

    // use a slot (ignores empty slots)
    void choose(int index) {
        if (index < 0 || index >= 4 || slots[index] == null) return;
        if (battle.isSevered(slots[index])) {   // GLITCH: torn out by SEVERANCE for the rest of the fight
            battle.showHint("THAT MOVE WAS SEVERED.");
            return;
        }
        if (battle.blockedAsRepeat(slots[index])) {   // BALANCE: no two BULWARK STANCEs / BRACEs in a row
            battle.showHint("CAN'T USE " + slots[index].getName() + " TWICE IN A ROW!");
            return;
        }
        if (locked(index)) {   // BALANCE: not learned yet -> refused, the window stays open
            battle.showHint("THIS MOVE UNLOCKS AT LV " + Balance.moveUnlockLevel(index) + "!");   // name stays hidden until learned
            return;
        }
        if (battle.ppLeft(slots[index]) == 0) {   // BALANCE: out of PP -> refused, the window stays open
            battle.showHint("NO PP LEFT FOR " + slots[index].getName() + "!");
            return;
        }
        interact(index);
    }

    @Override
    public void update() {
        if (!open) return;
        navigate(2);
        int n = BattleInput.number();
        if (n >= 0) cursor = n;
        if (BattleInput.back()) { close(); battle.backToCommands(); return; }
        if (BattleInput.info() && getHovered() != null && !locked(cursor) && !battle.isSevered(getHovered())) { battle.openAbilityInfo(getHovered()); return; }
        if (BattleInput.confirm()) choose(cursor);
    }

    static Color colorFor(MoveCategory c) {
        switch (c) {
            case MELEE: return BattleUI.BTN_MELEE;
            case SHIELD: return BattleUI.BTN_SHIELD;
            default: return BattleUI.BTN_RANGED;
        }
    }

    // GLITCH: a severed move: a box of dark static with a scrambled name that changes every few frames, nothing else
    private void drawSeveredBox(GraphicsHandler gh, int x, int y, int w, int h, int slot) {
        java.util.Random r = new java.util.Random((System.nanoTime() / 66_000_000L) * 17 + slot);
        Color[] noise = { new Color(10, 6, 16), new Color(42, 15, 61), new Color(90, 30, 138), new Color(138, 16, 32) };
        for (int yy = 0; yy < h; yy += 4)
            for (int xx = 0; xx < w; xx += 4) gh.drawFilledRectangle(x + xx, y + yy, 4, 4, noise[r.nextInt(noise.length)]);
        String chars = "#@%&?!$*/\\|<>=+~^";
        StringBuilder name = new StringBuilder();
        for (int k = 0; k < 9; k++) name.append(chars.charAt(r.nextInt(chars.length())));
        BattleUI.shadowText(gh, name.toString(), x + 42, y + 12, 2, new Color(220, 210, 230));
        gh.drawRectangle(x, y, w - 1, h - 1, slot == cursor ? BattleUI.GOLD : new Color(90, 30, 138), 2);
    }

    // BALANCE: the power shown for a move. SHIELD THROW includes the equipped shield (100% x (1 + block amount)).
    static String powerText(Ability a, double shieldBlock) {
        if (a.getEffect() == Ability.Effect.THROW) return Math.round(a.getPower() * (1 + shieldBlock) * 100) + "%";
        return a.getPowerText();
    }

    // BALANCE: "PP 3/5" (or "PP --" when unlimited)
    String ppText(Ability a) {
        int left = battle.ppLeft(a), max = battle.ppMax(a);
        return left < 0 ? "PP --" : "PP " + left + "/" + max;
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
            boolean empty = a != null && battle.ppLeft(a) == 0;   // BALANCE: no PP left -> greyed out
            boolean lock = a != null && locked(i);                 // BALANCE: not learned yet -> greyed out
            boolean repeat = a != null && battle.blockedAsRepeat(a);   // BALANCE: guard move used last turn -> greyed out
            boolean cut = a != null && battle.isSevered(a);            // GLITCH: severed -> a glitched box
            if (cut) {
                drawSeveredBox(gh, cx, cy, cellW, cellH, i);
                continue;
            }
            BattleUI.button(gh, cx, cy, cellW, cellH, colorFor(category), hover, a == null || empty || lock || repeat);
            // number badge
            gh.drawFilledRectangle(cx + 12, cy + 12, 22, 22, hover ? BattleUI.GOLD : new Color(255, 255, 255, 220));
            BattleUI.textCenter(gh, String.valueOf(i + 1), cx + 23, cy + 16, 2, BattleUI.INK);

            // long names (like BLOODFANG STRIKE) wrap onto two lines instead of running out of the button
            String name = a == null ? "---" : (lock ? "???" : a.getName());   // BALANCE: a locked move's name stays hidden
            int avail = cellW - 46 - 10, lines = 1;
            int tx = cx + 42;
            if (BattleUI.textW(name, 2) <= avail || name.indexOf(' ') < 0) {
                BattleUI.shadowText(gh, name, tx, cy + 12, 2, BattleUI.WHITE);
            } else {
                int split = name.indexOf(' ');
                BattleUI.shadowText(gh, name.substring(0, split), tx, cy + 12, 2, BattleUI.WHITE);
                BattleUI.shadowText(gh, name.substring(split + 1), tx, cy + 29, 2, BattleUI.WHITE);
                lines = 2;
            }
            int subY = cy + (lines == 2 ? 52 : 36);
            if (lock) {
                BattleUI.shadowText(gh, "LOCKED - LV " + Balance.moveUnlockLevel(i), tx, subY, 1, BattleUI.WHITE);   // BALANCE
            } else if (a != null) {
                // BALANCE: only power and PP on the button (details are on Y); greyed text when it can't be used right now
                String sub = "PWR " + powerText(a, shieldBlock) + "  " + ppText(a);
                BattleUI.shadowText(gh, sub, tx, subY, 1, empty || repeat ? new Color(255, 200, 190) : BattleUI.WHITE);
            }
            else BattleUI.shadowText(gh, "NOT ON THIS ITEM", tx, subY, 1, BattleUI.WHITE);
        }
    }
}