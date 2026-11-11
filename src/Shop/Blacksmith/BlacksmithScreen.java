package Shop.Blacksmith;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.Screen;
import Game.PlayerData;
import Item.Item;
import Item.ItemStats;
import Shop.ShopController;
import inventory.Inventory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static Screens.InventoryScreen.rarityColor;
import static Shop.ShopUI.BAD;
import static Shop.ShopUI.DIM;
import static Shop.ShopUI.GOOD;
import static Shop.ShopUI.PANEL_FILL;
import static Shop.ShopUI.SLOT_BORDER;
import static Shop.ShopUI.SLOT_FILL;
import static Shop.ShopUI.line;

// The blacksmith's window. UI only: every decision (selected group, which two copies are picked, who is kept, the fee, combining,
// the status message) lives in BlacksmithViewModel; this class maps keys to it and draws what it reports. Shown by ShopController,
// which calls onOpen(world) first (the blacksmith has no use for the world and ignores it).
//   UP / DOWN: previous / next item     LEFT: change copy A     RIGHT: change copy B     ENTER: combine     ESC: close
// Combining two copies of the same item keeps the better one (higher level, then more xp), repairs it to full durability and uses
// up the other copy. Left: the items that have a pair. Right: the two picked copies side by side, "KEEP" and "USED UP" marked.
public final class BlacksmithScreen extends Screen implements ShopController.Panel {
    private static final int LIST_W = 250;
    private static final int ROW = 56;
    private static final int ICON = 40;

    private static final Key[] INPUT_KEYS = {Key.ESC, Key.ENTER, Key.LEFT, Key.RIGHT, Key.UP, Key.DOWN};

    private final BlacksmithViewModel viewModel;
    private final Supplier<Inventory> inventory;   // only used to show which copy is equipped
    private final Runnable closeRequest;           // PlayLevelScreen passes () -> shopController.close()
    private final KeyLocker keyLocker = new KeyLocker();
    private CombineResult lastResult = null;

    public BlacksmithScreen(BlacksmithViewModel viewModel, Supplier<Inventory> inventory, Runnable closeRequest) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.closeRequest = Objects.requireNonNull(closeRequest, "closeRequest");
    }

    @Override
    public void initialize() {}

    @Override
    public void onOpen(int world) {
        viewModel.open();
        lastResult = null;
        // keys already held when the blacksmith opens (the ENTER that dismissed the greeting) must not count as presses
        for (Key key : INPUT_KEYS) keyLocker.lockKey(key);
    }

    // true once per physical key press (same pattern as InventoryScreen)
    private boolean isPressed(Key key) {
        if (Keyboard.isKeyUp(key)) keyLocker.unlockKey(key);
        if (Keyboard.isKeyDown(key) && !keyLocker.isKeyLocked(key)) { keyLocker.lockKey(key); return true; }
        return false;
    }

    // updates screen based on user input
    @Override
    public void update() {
        for (Key key : INPUT_KEYS) {
            if (!isPressed(key)) continue;
            switch (key) {
                case ESC -> { closeRequest.run(); return; }
                case UP -> viewModel.selectPreviousGroup();
                case DOWN -> viewModel.selectNextGroup();
                case LEFT -> viewModel.cycleFirst();
                case RIGHT -> viewModel.cycleSecond();
                case ENTER -> lastResult = viewModel.combine();
                default -> {}
            }
        }
    }

    // drawing
    @Override
    public void draw(GraphicsHandler g) {
        int screenW = BattleUI.W();
        int screenH = BattleUI.H();
        int panelX = 40, panelY = 40, panelW = screenW - 80, panelH = screenH - 80;
        int contentX = panelX + 30;
        int contentY = panelY + 80;
        int contentH = panelY + panelH - 130 - contentY;
        int rightX = contentX + LIST_W + 30;
        int rightW = panelX + panelW - 30 - rightX;

        g.drawFilledRectangle(0, 0, screenW, screenH, DIM);
        g.drawFilledRectangleWithBorder(panelX, panelY, panelW, panelH, PANEL_FILL, Color.black, 3);

        BattleUI.shadowText(g, "BLACKSMITH", panelX + 30, panelY + 24, 3, BattleUI.WHITE);
        String gold = "GOLD " + PlayerData.getCurrency();
        BattleUI.shadowText(g, gold, panelX + panelW - 30 - BattleUI.textW(gold, 2), panelY + 28,
                2, BattleUI.GOLD);

        List<List<Item>> groups = viewModel.getGroups();
        if (groups.isEmpty()) {
            BattleUI.text(g, "NOTHING TO COMBINE", contentX, contentY + 10, 2, BattleUI.GREY);
            BattleUI.text(g, "BRING TWO COPIES OF THE SAME ITEM", contentX, contentY + 40, 2, BattleUI.GREY);
        } else {
            drawGroupList(g, groups, contentX, contentY, LIST_W, contentH);
            drawComparison(g, rightX, contentY, rightW, contentH);
        }
        drawBottomBar(g, panelX, panelY, panelW, panelH);
    }

    // the items that have a pair, scrolled so the selected one is always on screen
    private void drawGroupList(GraphicsHandler g, List<List<Item>> groups, int x, int y, int w, int h) {
        g.drawFilledRectangleWithBorder(x, y, w, h, SLOT_FILL, SLOT_BORDER, 2);
        int selected = viewModel.getSelectedGroupIndex();
        int visible = Math.max(1, (h - 8) / ROW);
        int top = Math.clamp(selected - visible / 2, 0, Math.max(0, groups.size() - visible));
        int nameChars = Math.max(1, (w - ICON - 28) / (6 * 2));   // every letter is 6 px wide at scale 1

        for (int i = top; i < Math.min(groups.size(), top + visible); i++) {
            List<Item> group = groups.get(i);
            Item sample = group.get(0);
            int rowY = y + 4 + (i - top) * ROW;
            g.drawFilledRectangle(x + 8, rowY + (ROW - ICON) / 2, ICON, ICON, rarityColor(sample.getItemRarity()));
            drawSprite(g, sample, x + 8, rowY + (ROW - ICON) / 2, ICON);

            int ty = rowY + 8;
            List<String> lines = BattleUI.wrap(BattleUI.prettyName(sample.getItemName()), nameChars);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                BattleUI.text(g, lines.get(l), x + ICON + 18, ty, 2, rarityColor(sample.getItemRarity()));
                ty += BattleUI.font().getCapHeight(2) + 4;
            }
            BattleUI.text(g, group.size() + " COPIES", x + ICON + 18, ty + 2, 1, BattleUI.GREY);

            if (i == selected) g.drawRectangle(x + 3, rowY, w - 7, ROW - 2, Color.white, 3);
        }
        if (top > 0) BattleUI.text(g, "^ MORE", x + w - 8 - BattleUI.textW("^ MORE", 1), y + 4,
                1, BattleUI.GREY);
        if (top + visible < groups.size()) {
            BattleUI.text(g, "v MORE", x + w - 8 - BattleUI.textW("v MORE", 1), y + h - 12,
                    1, BattleUI.GREY);
        }
    }

    // the two picked copies side by side
    private void drawComparison(GraphicsHandler g, int x, int y, int w, int h) {
        int cardW = (w - 20) / 2;
        int cardH = h - 24;
        Item first = viewModel.getFirst();
        Item second = viewModel.getSecond();
        Item keeper = viewModel.getKeeper();
        drawCard(g, "A", first, first == keeper, x, y, cardW, cardH);
        drawCard(g, "B", second, second == keeper, x + cardW + 20, y, cardW, cardH);

        Inventory pack = inventory.get();
        Item fodder = viewModel.getFodder();
        if (fodder != null && pack.isItemEquipped(fodder)) {
            BattleUI.text(g, "THE EQUIPPED COPY IS USED UP, THE KEPT COPY TAKES ITS SLOT", x, y + cardH + 8,
                    1, BattleUI.GREY);
        }
    }

    private void drawCard(GraphicsHandler g, String label, Item item, boolean kept, int x, int y, int w, int h) {
        g.drawFilledRectangleWithBorder(x, y, w, h, SLOT_FILL, kept ? GOOD : SLOT_BORDER, kept ? 3 : 2);
        if (item == null) return;
        int tx = x + 12;
        int width = w - 24;
        int bottom = y + h - 8;
        ItemStats stats = item.getItemStats();

        // the label is small so it can never run into the KEEP / USED UP tag, even in a narrow card
        BattleUI.text(g, "COPY " + label, tx, y + 13, 1, BattleUI.GREY);
        String tag = kept ? "KEEP" : "USED UP";
        BattleUI.text(g, tag, x + w - 12 - BattleUI.textW(tag, 2), y + 10, 2, kept ? GOOD : BAD);

        int ty = y + 10 + BattleUI.font().getCapHeight(2) + 14;
        int icon = 56;
        g.drawFilledRectangle(tx, ty, icon, icon, rarityColor(item.getItemRarity()));
        drawSprite(g, item, tx, ty, icon);
        int nameChars = Math.max(1, (width - icon - 10) / (6 * 2));
        int ny = ty + 2;
        List<String> lines = BattleUI.wrap(BattleUI.prettyName(item.getItemName()), nameChars);
        for (int l = 0; l < Math.min(3, lines.size()); l++) {
            BattleUI.text(g, lines.get(l), tx + icon + 10, ny, 2, rarityColor(item.getItemRarity()));
            ny += BattleUI.font().getCapHeight(2) + 4;
        }
        ty += icon + 14;

        ty = line(g, "LV " + stats.getLvl() + (stats.isMaxLvl() ? " MAX" : ""), tx, ty, width, Color.white, bottom);
        if (!stats.isMaxLvl()) ty = line(g, "XP " + stats.getXp() + "/" + stats.getXpToNextLvl(), tx, ty, width, Color.lightGray, bottom);
        ty = line(g, "DUR " + stats.getDurability() + "/" + stats.getItemMaxDurability(), tx, ty, width, Color.white, bottom);
        if (ty + 14 <= bottom) {
            BattleUI.bar(g, tx, ty, width, 14, (float) stats.getDurabilityFraction(), BattleUI.hpColor((float) stats.getDurabilityFraction()));
            ty += 14 + 8;
        }
        if (inventory.get().isItemEquipped(item)) line(g, "EQUIPPED", tx, ty, width, BattleUI.GOLD, bottom);
    }

    private void drawBottomBar(GraphicsHandler g, int panelX, int panelY, int panelW, int panelH) {
        int x = panelX + 30;
        int y = panelY + panelH - 118;
        Item keeper = viewModel.getKeeper();
        if (keeper != null) {
            int fee = viewModel.getFee();
            boolean affordable = PlayerData.getCurrency() >= fee;
            ItemStats stats = keeper.getItemStats();
            BattleUI.text(g, "FEE " + fee, x, y, 2, affordable ? BattleUI.GOLD : BAD);
            BattleUI.text(g, "KEEPS LV " + stats.getLvl() + "   DUR " + stats.getDurability() + " > " + stats.getItemMaxDurability(),
                    x + 190, y, 2, BattleUI.WHITE);
        }
        String message = viewModel.getMessage();
        if (!message.isEmpty()) {
            BattleUI.text(g, message, x, y + 34, 2, lastResult == CombineResult.SUCCESS ? GOOD : BAD);
        }
        // key hints, two lines at scale 2 so they are easy to read (one line at this size would be wider than the panel)
        BattleUI.shadowText(g, "UP/DOWN: ITEM   LEFT: COPY A   RIGHT: COPY B", x, panelY + panelH - 56,
                2, BattleUI.WHITE);
        BattleUI.shadowText(g, "ENTER: COMBINE   ESC: CLOSE", x, panelY + panelH - 34, 2, BattleUI.WHITE);
    }

    // helpers
    private static void drawSprite(GraphicsHandler g, Item item, int x, int y, int size) {
        if (item.getItemLook() == null) return;
        BufferedImage sprite = item.getItemLook().getSprite(0, 0);
        g.drawImage(sprite, x, y, size, size);
    }
}
