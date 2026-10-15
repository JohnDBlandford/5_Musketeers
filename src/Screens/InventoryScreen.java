package Screens;

import BattleSystem.BattleUI;
import Engine.*;
import Item.Item;
import SpriteFont.SpriteFont;
import inventory.EquipSlot;
import inventory.Inventory;
import Item.ItemStats;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// The inventory, drawn with the same pixel font (VCR OSD Mono) as the title screen and the battle screens.
public class InventoryScreen extends Screen {
    private KeyLocker keyLocker = new KeyLocker();
    private PlayLevelScreen playLevelScreen;
    private SpriteFont instructions;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int SLOT = 56;
    private static final int GAP = 8;
    private static final int PITCH = SLOT + GAP;
    private static final Color SLOT_FILL = new Color(35, 35, 35);
    private static final Color SLOT_BORDER = new Color(90, 90, 90);
    private static final Color PANEL_FILL = new Color(60, 60, 60);
    private static final Color DIM = new Color(0, 0, 0, 140);
    private static final Color GOLD = new Color(230, 190, 40); // a value that has stopped growing with level
    private static final Color BAR_BACK = new Color(20, 20, 20);
    private static final Color XP_COLOR = new Color(80, 140, 220);
    private static final Color DURABILITY_HIGH = new Color(80, 170, 80);
    private static final Color DURABILITY_MID = new Color(220, 190, 50);
    private static final Color DURABILITY_LOW = new Color(200, 60, 60);
    private static final Font NAME_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font BODY_FONT = new Font("Arial", Font.PLAIN, 14);;
    private static final String INSTRUCTIONS = "ARROWS: MOVE   ENTER: EQUIP / UNEQUIP   ESC: CLOSE";
    private static final int NAME_SCALE = 2;   // item name
    private static final int BODY_SCALE = 2;   // rarity + stats
    private static final int HELP_SCALE = 2;   // instructions line
    private int row, col;
    private int panelX, panelY, panelW, panelH, slotsY, equipX, gridX, detailX, detailW, detailH;
    private final int screenWidth = ScreenManager.getScreenWidth();
    private final int screenHeight = ScreenManager.getScreenHeight();
    private static final boolean DEBUG_KEYS = true;
    // for debug: W subtracts 10 durability, I gives 500 xp to selected item
    private static final Key[] INPUT_KEYS = DEBUG_KEYS ? new Key[]{ Key.ESC, Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT, Key.ENTER, Key.I, Key.W }
                                                                                       : new Key[]{Key.ESC, Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT, Key.ENTER};

    public InventoryScreen(PlayLevelScreen playLevelScreen) {
        this.playLevelScreen = playLevelScreen;
        initialize();
    }

    @Override
    public void initialize() {
        instructions = new SpriteFont("Arrows: move   ENTER: equip / unequip   ESC: close", 80, 70, "Arial", 25, Color.white);
        instructions.setOutlineColor(Color.black);
        keyLocker.lockKey(Key.ESC);
        keyLocker.lockKey(Key.I);
        keyLocker.lockKey(Key.W);
        panelX = 40;
        panelY = 40;
        panelW = screenWidth - 80;
        panelH = screenHeight - 80;
        slotsY = panelY + 70;
        equipX = panelX + 30;
        gridX = equipX + SLOT + 40;
        detailX = gridX + COLS * PITCH - GAP + 30;
        detailW = panelX + panelW - 30 - detailX;
        detailH = panelY + panelH - 50 - slotsY;
    }

    // this is true once per physical key press
    private boolean isPressed(Key key) {
        // if the key is up, unlock it
        if (Keyboard.isKeyUp(key)) keyLocker.unlockKey(key);
        // if key is down and not yet locked, lock it
        if (Keyboard.isKeyDown(key) && !keyLocker.isKeyLocked(key)) { keyLocker.lockKey(key); return true; }
        return false;
    }

    @Override
    public void update() {
        for (Key key : INPUT_KEYS) {
            if (!isPressed(key)) continue;
            switch (key) {
                case ESC -> { playLevelScreen.closeInventory(); return; }
                case UP -> row = Math.max(0, row - 1);
                case DOWN -> row = Math.min(ROWS - 1, row + 1);
                case LEFT -> col = Math.max(0, col - 1);
                case RIGHT -> col = Math.min(COLS - 1, col + 1);
                case ENTER -> playLevelScreen.getInventory().toggleItemEquip(row * COLS + col);
                case W -> debugDmgSelected();
                case I -> debugAddXpToSelected();
                default -> {}
            }
        }
    }

    private Item selectedItem() {
        List<Item> items = playLevelScreen.getInventory().getAllItems();
        int idx = row * COLS + col;
        return idx < items.size() ? items.get(idx) : null;
    }

    // debug only methods
    private void debugDmgSelected() {
        Item item = selectedItem();
        if (item != null) item.getItemStats().damageItem(10);
    }

    private void debugAddXpToSelected() {
        Item item = selectedItem();
        if (item != null) item.getItemStats().addXp(500);
    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        Inventory inv = playLevelScreen.getInventory();
        graphicsHandler.drawFilledRectangle(0, 0, screenWidth, screenHeight, DIM);
        graphicsHandler.drawFilledRectangleWithBorder(panelX, panelY, panelW, panelH, PANEL_FILL, Color.black, 3);
        BattleUI.shadowText(graphicsHandler, INSTRUCTIONS, panelX + 30, panelY + 24, HELP_SCALE, BattleUI.WHITE);

        for (int i = 0; i < EquipSlot.values().length; i++)
            drawEquipSlot(graphicsHandler, inv, equipX, slotsY + i * PITCH, EquipSlot.values()[i]);
        List<Item> pack = inv.getAllItems();
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                int idx = r * COLS + c;
                drawSlot(graphicsHandler, inv, gridX + c * PITCH, slotsY + r * PITCH, idx < pack.size() ? pack.get(idx) : null);
            }

        graphicsHandler.drawRectangle(gridX + col * PITCH - 3, slotsY + row * PITCH - 3,
                SLOT + 6, SLOT + 6, Color.white, 3);   // last, so nothing covers it
        drawDetails(graphicsHandler, selectedItem());
    }

    private void drawDetails(GraphicsHandler graphicsHandler, Item item) {
        graphicsHandler.drawFilledRectangleWithBorder(detailX, slotsY, detailW, detailH, SLOT_FILL, SLOT_BORDER, 2);
        if (item == null) return;
        int x = detailX + 12;
        int y = slotsY + 12;
        int nameLineH = BattleUI.font().getCapHeight(NAME_SCALE) + 8;
        int bodyLineH = BattleUI.font().getCapHeight(BODY_SCALE) + 8;
        int nameChars = Math.max(1, (detailW - 24) / (6 * NAME_SCALE));   // every letter is 6 px wide at scale 1
        String name = item.getItemName().replaceAll("(?<=[a-z])(?=[A-Z])", " ");
        for (String line : BattleUI.wrap(name, nameChars)) {
            BattleUI.text(graphicsHandler, line, x, y, NAME_SCALE, rarityColor(item));
            y += nameLineH;
        }
        graphicsHandler.drawString(item.getItemRarity().name(), x, y, BODY_FONT, Color.lightGray);
        for (Map.Entry<Item.ItemBuffs, Double> entry : item.getScaledBuffs().entrySet()) {
            if (entry.getKey() == Item.ItemBuffs.SPD_BOOST || entry.getKey() == Item.ItemBuffs.BONUS_MAX_HP) continue;
            y += 20;
            graphicsHandler.drawString(entry.getKey() + "  " + formatBuff(entry.getKey(), entry.getValue()), x, y, BODY_FONT,
                    item.isBuffCapped(entry.getKey()) ? GOLD : Color.white);
        }
        drawStats(graphicsHandler, item, x, y, detailW - 24);
    }

    // this method draws everything below the dividing line: lvl, xp, durability, then atk, def, spd and bhp
    // y is the baseline of the last line drawn above, so this follows however tall the name and buffs were
    private void drawStats(GraphicsHandler g, Item item, int x, int y, int width) {
        ItemStats stats = item.getItemStats();

        y += 14;
        g.drawFilledRectangle(x, y, width, 2, SLOT_BORDER);   // the divider between the old and the new

        y += 24;
        g.drawString("Level  " + stats.getLvl() + " / " + ItemStats.MAX_LVL, x, y, BODY_FONT, Color.white);

        y += 20;
        g.drawString(stats.isMaxLvl() ? "XP  MAX" : "XP  " + stats.getXp() + " / " + stats.getXpToNextLvl(),
                x, y, BODY_FONT, Color.white);
        drawBar(g, x, y + 6, width, 8, stats.getXpBarFullness(), XP_COLOR);
        y += 14;

        y += 20;
        g.drawString("Durability  " + stats.getDurability() + " / " + stats.getItemMaxDurability(),
                x, y, BODY_FONT, Color.white);
        drawBar(g, x, y + 6, width, 8, stats.getDurabilityFraction(), drawDurabilityColor(stats.getDurabilityFraction()));
        y += 14;

        // every stat is always shown, a 0 is a real value and never "N/A"
        y += 24;
        g.drawString("Attack  " + item.getAttack(), x, y, BODY_FONT, Color.white);
        y += 20;
        g.drawString("Defense  " + item.getDefense(), x, y, BODY_FONT, Color.white);
        y += 20;
        g.drawString("Speed  " + formatPercent(item.getItemSpeed()), x, y, BODY_FONT,
                item.isBuffCapped(Item.ItemBuffs.SPD_BOOST) ? GOLD : Color.white);
        y += 20;
        g.drawString("Bonus HP  " + formatBuff(Item.ItemBuffs.BONUS_MAX_HP, item.getItemBonusMaxHp()), x, y, BODY_FONT,
                item.isBuffCapped(Item.ItemBuffs.BONUS_MAX_HP) ? GOLD : Color.white);
    }

    // draws the bar  for the lvl and xp for each item
    private static void drawBar(GraphicsHandler g, int x, int y, int width, int height, double fraction, Color fill) {
        g.drawFilledRectangleWithBorder(x, y, width, height, BAR_BACK, SLOT_BORDER, 1);
        int filled = (int) Math.round((width - 2) * Math.clamp(fraction, 0.0, 1.0));
        if (filled > 0) g.drawFilledRectangle(x + 1, y + 1, filled, height - 2, fill);
        y += 4;
        BattleUI.text(g, item.getItemRarity().name(), x, y, BODY_SCALE, Color.lightGray);
        for (var e : item.getTotalItemBuffs().entrySet()) {
            y += bodyLineH;
            String line = (e.getKey() + "  " + e.getValue()).replace('_', ' ');
            BattleUI.text(graphicsHandler, line, x, y, BODY_SCALE, Color.white);
        }
    }

    // draws the durability lvls for each item so you can quickly look at it
    private static Color drawDurabilityColor(double fraction) {
        if (fraction > 0.5) return DURABILITY_HIGH;
        if (fraction > 0.25) return DURABILITY_MID;
        return DURABILITY_LOW;
    }

    // formats the buffs to make it easier to see, flat buffs are stored as the buff val / 100, everything else is a percentage
    private static String formatBuff(Item.ItemBuffs buff, double value) {
        return Item.isFlatBuff(buff) ? "+" + Math.round(value * 100.0) : formatPercent(value);
    }

    // formats the percentages to one decimal place and removes the 0.0
    // ask about Locale usage
    private static String formatPercent(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value * 100.0);
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        return text + "%";
    }

    // draws an individual inventory slot
    private void drawSlot(GraphicsHandler g, Inventory inv, int x, int y, Item item) {
        g.drawFilledRectangleWithBorder(x, y, SLOT, SLOT, SLOT_FILL, SLOT_BORDER, 2);
        if (item == null) return;
        g.drawFilledRectangle(x + 4, y + 4, SLOT - 8, SLOT - 8, rarityColor(item));
        if (item.getItemLook() != null)
            g.drawImage(item.getItemLook().getSprite(0, 0), x + 4, y + 4, SLOT - 8, SLOT - 8);
        if (inv.isItemEquipped(item)) // gold corner marker
            g.drawFilledRectangleWithBorder(x + SLOT - 14, y + 2, 12, 12, new Color(230, 190, 40), Color.black, 1);
    }

    private void drawEquipSlot(GraphicsHandler graphicsHandler, Inventory inv, int x, int y, EquipSlot slot) {
        Item item = inv.getEquippedItem(slot);
        drawSlot(graphicsHandler, inv, x, y, item);
        if (item == null) {
            int scale = 3;
            BattleUI.textCenter(graphicsHandler, slot.name().substring(0, 1), x + SLOT / 2, y + (SLOT - BattleUI.font().getCapHeight(scale)) / 2, scale, Color.gray);
        }
    }

    // rarity color assigner
    private static Color rarityColor(Item item) {
        return switch (item.getItemRarity()) {
            case UNCOMMON -> new Color(80, 170, 80);
            case EPIC -> new Color(150, 80, 200);
            case LEGENDARY -> new Color(230, 150, 40);
            default -> new Color(130, 130, 130);
        };
    }
}