package Screens;

import Engine.*;
import Item.Item;
import SpriteFont.SpriteFont;
import inventory.EquipSlot;
import inventory.Inventory;

import java.awt.*;
import java.util.List;

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
    private static final Font NAME_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font BODY_FONT = new Font("Arial", Font.PLAIN, 14);;
    private int row, col;
    private int panelX, panelY, panelW, panelH, slotsY, equipX, gridX, detailX, detailW, detailH;
    private final int screenWidth = ScreenManager.getScreenWidth();
    private final int screenHeight = ScreenManager.getScreenHeight();
    private static final Key[] INPUT_KEYS = { Key.ESC, Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT, Key.ENTER };

    public InventoryScreen(PlayLevelScreen playLevelScreen) {
        this.playLevelScreen = playLevelScreen;
        initialize();
    }

    @Override
    public void initialize() {
        instructions = new SpriteFont("Press I to enter and ESC exit the inventory", 150, 100, "Arial", 25, Color.white);
        instructions.setOutlineColor(Color.black);
        instructions.setOutlineThickness(2);
        keyLocker.lockKey(Key.ESC);
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
                default -> {}
            }
        }
    }

    private Item selectedItem() {
        List<Item> items = playLevelScreen.getInventory().getAllItems();
        int idx = row * COLS + col;
        return idx < items.size() ? items.get(idx) : null;
    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        Inventory inv = playLevelScreen.getInventory();
        graphicsHandler.drawFilledRectangle(0, 0, ScreenManager.getScreenWidth(), ScreenManager.getScreenHeight(), DIM);
        graphicsHandler.drawFilledRectangleWithBorder(panelX, panelY, panelW, panelH, PANEL_FILL, Color.black, 3);
        instructions.draw(graphicsHandler);

        for (int i = 0; i < EquipSlot.values().length; i++)
            drawSlot(graphicsHandler, inv, equipX, slotsY + i * PITCH, inv.getEquippedItem(EquipSlot.values()[i]));
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
        int y = slotsY + 28;
        graphicsHandler.drawString(item.getItemName().replaceAll("(?<=[a-z])(?=[A-Z])", " "), x, y, NAME_FONT, rarityColor(item));
        y += 22;
        graphicsHandler.drawString(item.getItemRarity().name(), x, y, BODY_FONT, Color.lightGray);
        for (var e : item.getTotalItemBuffs().entrySet()) {
            y += 20;
            graphicsHandler.drawString(e.getKey() + "  " + e.getValue(), x, y, BODY_FONT, Color.white);
        }
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
