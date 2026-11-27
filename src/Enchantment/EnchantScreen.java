package Enchantment;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.Screen;
import Enchantment.Enchantment.EnchantmentType;
import Enchantment.ItemEnchants.ApplyResult;
import Item.Item;
import Shop.ShopController;
import inventory.Inventory;
import inventory.Inventory.BookStack;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Supplier;

import static Screens.InventoryScreen.rarityColor;
import static Shop.ShopUI.*;

// The enchant table's window. UI only: every decision (the two cursors, the mode, what a press does, the status message) lives in
// EnchantViewModel; this class maps keys to it and draws what it reports. Shown by ShopController, which calls onOpen(world) first
// (the world is the highest book level this table can combine up to).
//   UP / DOWN: previous / next gear     LEFT / RIGHT: previous / next book     SPACE: apply or combine     ENTER: do it     ESC: close
// APPLY puts the selected book on the selected gear (the book is used up). COMBINE turns two of the selected book into one of the
// next level and needs no gear. Left: your gear with what it carries. Right: your books and a card for what ENTER would do.
public final class EnchantScreen extends Screen implements ShopController.Panel {
    private static final int LIST_W = 250;
    private static final int ROW = 56;
    private static final int ICON = 40;

    private static final Key[] INPUT_KEYS = {Key.ESC, Key.ENTER, Key.SPACE, Key.LEFT, Key.RIGHT, Key.UP, Key.DOWN};

    private final EnchantViewModel viewModel;
    private final Supplier<Inventory> inventory;   // only used to show which piece of gear is equipped
    private final Runnable closeRequest;           // PlayLevelScreen passes () -> shopController.close()
    private final KeyLocker keyLocker = new KeyLocker();
    private boolean lastSucceeded = false;         // colours the status message

    public EnchantScreen(EnchantViewModel viewModel, Supplier<Inventory> inventory, Runnable closeRequest) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel not set");
        this.inventory = Objects.requireNonNull(inventory, "inventory not supplied");
        this.closeRequest = Objects.requireNonNull(closeRequest, "closeRequest not set. Attach a Runnable");
    }

    @Override
    public void initialize() {}

    @Override
    public void onOpen(int world) {
        viewModel.open(world);
        lastSucceeded = false;
        // keys already held when the table opens (the ENTER that dismissed the greeting) must not count as presses
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
                case UP -> viewModel.selectPreviousItem();
                case DOWN -> viewModel.selectNextItem();
                case LEFT -> viewModel.selectPreviousBook();
                case RIGHT -> viewModel.selectNextBook();
                case SPACE -> { viewModel.toggleMode(); lastSucceeded = false; }
                case ENTER -> confirm();
                default -> {}
            }
        }
    }

    private void confirm() {
        if (viewModel.getMode() == EnchantViewModel.Mode.APPLY) {
            ApplyResult result = viewModel.applyEnchant();
            lastSucceeded = result == ApplyResult.APPLIED || result == ApplyResult.REPLACED;
        } else {
            lastSucceeded = viewModel.combineBooks() == CombineBooksResult.SUCCESS;
        }
    }

    // drawing
    @Override
    public void draw(GraphicsHandler g) {
        Objects.requireNonNull(g, "graphics cannot be null");
        int screenW = BattleUI.W();
        int screenH = BattleUI.H();
        int panelX = 40, panelY = 40, panelW = screenW - 80, panelH = screenH - 80;
        int contentX = panelX + 30;
        int contentY = panelY + 100;
        int contentH = panelY + panelH - 130 - contentY;
        int rightX = contentX + LIST_W + 30;
        int rightW = panelX + panelW - 30 - rightX;
        boolean applying = viewModel.getMode() == EnchantViewModel.Mode.APPLY;

        g.drawFilledRectangle(0, 0, screenW, screenH, getDimColor());
        g.drawFilledRectangleWithBorder(panelX, panelY, panelW, panelH, getPanelFillColor(), Color.black, 3);

        BattleUI.shadowText(g, "ENCHANT TABLE", panelX + 30, panelY + 24, 3, BattleUI.WHITE);
        String cap = "MAX BOOK LEVEL " + Enchantment.toRoman(viewModel.getWorldCap());
        BattleUI.shadowText(g, cap, panelX + panelW - 30 - BattleUI.textW(cap, 2), panelY + 28, 2,
                BattleUI.GOLD);

        // the two modes side by side, the active one lit
        int tabY = panelY + 66;
        BattleUI.text(g, "APPLY", contentX, tabY, 2, applying ? BattleUI.WHITE : BattleUI.GREY);
        BattleUI.text(g, "COMBINE", contentX + 110, tabY, 2, applying ? BattleUI.GREY : BattleUI.WHITE);
        g.drawFilledRectangle(contentX + (applying ? 0 : 110), tabY + BattleUI.font().getCapHeight(2) + 4,
                BattleUI.textW(applying ? "APPLY" : "COMBINE", 2), 3, BattleUI.GOLD);

        drawGearList(g, viewModel.getItems(), contentX, contentY, LIST_W, contentH);
        // combining uses no gear, so the gear list is dimmed while it is the active mode
        if (!applying) g.drawFilledRectangle(contentX, contentY, LIST_W, contentH, new Color(0, 0, 0, 120));

        int listH = Math.min(contentH, ROW * 3 + 12);
        drawBookList(g, viewModel.getBooks(), rightX, contentY, rightW, listH);
        drawCard(g, rightX, contentY + listH + 12, rightW, contentH - listH - 12);
        drawBottomBar(g, panelX, panelY, panelW, panelH);
    }

    // the gear, scrolled so the selected one is always on screen
    private void drawGearList(GraphicsHandler g, List<Item> items, int x, int y, int w, int h) {
        Objects.requireNonNull(g, "graphics cannot be null");
        Objects.requireNonNull(items, "items cannot be null");
        g.drawFilledRectangleWithBorder(x, y, w, h, getSlotFillColor(), getSlotBorderColor(), 2);
        if (items.isEmpty()) {
            BattleUI.text(g, "NO GEAR", x + 12, y + 12, 2, BattleUI.GREY);
            return;
        }
        int selected = viewModel.getSelectedItemIndex();
        int visible = Math.max(1, (h - 8) / ROW);
        int top = Math.clamp(selected - visible / 2, 0, Math.max(0, items.size() - visible));
        int nameChars = Math.max(1, (w - ICON - 28) / (6 * 2));   // every letter is 6 px wide at scale 1
        Inventory pack = inventory.get();

        for (int i = top; i < Math.min(items.size(), top + visible); i++) {
            Item item = items.get(i);
            int rowY = y + 4 + (i - top) * ROW;
            g.drawFilledRectangle(x + 8, rowY + (ROW - ICON) / 2, ICON, ICON, rarityColor(item.getItemRarity()));
            drawSprite(g, item, x + 8, rowY + (ROW - ICON) / 2, ICON);

            int ty = rowY + 6;
            List<String> lines = BattleUI.wrap(BattleUI.prettyName(item.getItemName()), nameChars);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                BattleUI.text(g, lines.get(l), x + ICON + 18, ty, 2, rarityColor(item.getItemRarity()));
                ty += BattleUI.font().getCapHeight(2) + 4;
            }
            String carried = pack.isItemEquipped(item) ? "EQUIPPED  " + enchantSummary(item) : enchantSummary(item);
            BattleUI.text(g, fit(carried, (w - ICON - 28) / 6), x + ICON + 18, ty + 2, 1,
                    item.getEnchants().isEmpty() ? BattleUI.GREY : BattleUI.WHITE);

            if (i == selected) g.drawRectangle(x + 3, rowY, w - 7, ROW - 2, Color.white, 3);
        }
        drawMoreMarkers(g, x, y, w, h, top, visible, items.size());
    }

    // the book stacks, scrolled the same way
    private void drawBookList(GraphicsHandler g, List<BookStack> books, int x, int y, int w, int h) {
        Objects.requireNonNull(g, "graphics cannot be null");
        Objects.requireNonNull(books, "book list cannot be null");
        g.drawFilledRectangleWithBorder(x, y, w, h, getSlotFillColor(), getSlotBorderColor(), 2);
        if (books.isEmpty()) {
            BattleUI.text(g, "NO BOOKS", x + 12, y + 12, 2, BattleUI.GREY);
            BattleUI.text(g, "FIND THEM IN CHESTS OR BUY THEM FROM THE MERCHANT", x + 12, y + 40,
                    1, BattleUI.GREY);
            return;
        }
        int selected = viewModel.getSelectedBookIndex();
        int visible = Math.max(1, (h - 8) / ROW);
        int top = Math.clamp(selected - visible / 2, 0, Math.max(0, books.size() - visible));
        int nameChars = Math.max(1, (w - ICON - 28 - 60) / (6 * 2));

        for (int i = top; i < Math.min(books.size(), top + visible); i++) {
            BookStack stack = books.get(i);
            EnchantBook book = stack.book();
            int rowY = y + 4 + (i - top) * ROW;
            int tileY = rowY + (ROW - ICON) / 2;
            g.drawFilledRectangle(x + 8, tileY, ICON, ICON, rarityColor(book.getItemRarity()));
            // PLACEHOLDER until the enchanted book sprite exists
            BattleUI.textCenter(g, "B", x + 8 + ICON / 2, tileY + (ICON - BattleUI.font()
                    .getCapHeight(3)) / 2, 3, Color.black);

            int ty = rowY + 10;
            List<String> lines = BattleUI.wrap(BattleUI.prettyName(book.getItemName()), nameChars);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                BattleUI.text(g, lines.get(l), x + ICON + 18, ty, 2, rarityColor(book.getItemRarity()));
                ty += BattleUI.font().getCapHeight(2) + 4;
            }
            String qty = "x" + stack.qty();
            BattleUI.text(g, qty, x + w - 12 - BattleUI.textW(qty, 2), rowY + (ROW - BattleUI.font()
                            .getCapHeight(2)) / 2, 2, BattleUI.WHITE);

            if (i == selected) g.drawRectangle(x + 3, rowY, w - 7, ROW - 2, Color.white, 3);
        }
        drawMoreMarkers(g, x, y, w, h, top, visible, books.size());
    }

    // what ENTER would do with the selection, and whether it would work
    private void drawCard(GraphicsHandler g, int x, int y, int w, int h) {
        Objects.requireNonNull(g, "graphics cannot be null");
        g.drawFilledRectangleWithBorder(x, y, w, h, getSlotFillColor(), getSlotBorderColor(), 2);
        BookStack stack = viewModel.getSelectedBook();
        if (stack == null) return;
        EnchantBook book = stack.book();
        EnchantmentType type = book.getEnchantment();
        int tx = x + 12;
        int width = w - 24;
        int bottom = y + h - 8;
        int ty = y + 10;

        ty = line(g, book.getDisplayName().toUpperCase(Locale.ROOT) + "   " +
                        formatPercent(EnchantmentConfig.getEffect(type, book.getLevel())),
                tx, ty, width, rarityColor(book.getItemRarity()), bottom);
        ty = line(g, "FITS " + slotNames(type), tx, ty, width, Color.lightGray, bottom);
        ty += 4;

        if (viewModel.getMode() == EnchantViewModel.Mode.APPLY) {
            Item item = viewModel.getSelectedItem();
            if (item == null) { line(g, "NO GEAR TO ENCHANT", tx, ty, width, getBadColor(), bottom); return; }
            ty = line(g, "ONTO " + BattleUI.prettyName(item.getItemName()).toUpperCase(Locale.ROOT), tx, ty, width,
                    Color.white, bottom);
            int now = item.getEnchants().getLevel(type);
            if (!type.appliesTo(item.getEquipSlot())) {
                line(g, "DOES NOT FIT THIS ITEM", tx, ty, width, getBadColor(), bottom);
            } else if (now >= book.getLevel()) {
                line(g, "HAS " + type.getDisplayName().toUpperCase(Locale.ROOT) + " " + Enchantment.toRoman(now) +
                        " ALREADY", tx, ty, width, getBadColor(), bottom);
            } else {
                String change = now == 0 ? "ADDS LEVEL " + Enchantment.toRoman(book.getLevel())
                        : "LEVEL " + Enchantment.toRoman(now) + " TO " + Enchantment.toRoman(book.getLevel());
                line(g, change, tx, ty, width, getGoodColor(), bottom);
            }
        } else {
            EnchantBook target = viewModel.getCombineTarget();
            if (target == null) {
                String why = book.getLevel() >= Enchantment.getMaxLevel() ? "ALREADY THE HIGHEST LEVEL"
                        : "THIS WORLD ALLOWS UP TO LEVEL " + Enchantment.toRoman(viewModel.getWorldCap());
                line(g, why, tx, ty, width, getBadColor(), bottom);
            } else if (stack.qty() < 2) {
                line(g, "NEED TWO OF THIS BOOK", tx, ty, width, getBadColor(), bottom);
            } else {
                line(g, "2 BOOKS MAKE " + target.getDisplayName().toUpperCase(Locale.ROOT), tx, ty, width, getGoodColor(), bottom);
            }
        }
    }

    private void drawBottomBar(GraphicsHandler g, int panelX, int panelY, int panelW, int panelH) {
        int x = panelX + 30;
        String message = viewModel.getMessage();
        if (!message.isEmpty()) {
            BattleUI.text(g, message, x, panelY + panelH - 118 + 34, 2, lastSucceeded ? getGoodColor() : getBadColor());
        }
        // key hints, two lines at scale 2 so they are easy to read (one line at this size would be wider than the panel)
        BattleUI.shadowText(g, "UP/DOWN: GEAR   LEFT/RIGHT: BOOK   SPACE: MODE", x, panelY + panelH - 56,
                2, BattleUI.WHITE);
        String confirm = viewModel.getMode() == EnchantViewModel.Mode.APPLY ? "ENTER: APPLY" : "ENTER: COMBINE";
        BattleUI.shadowText(g, confirm + "   ESC: CLOSE", x, panelY + panelH - 34, 2, BattleUI.WHITE);
    }

    // helpers
    private static void drawMoreMarkers(GraphicsHandler g, int x, int y, int w, int h, int top, int visible, int total) {
        if (top > 0) BattleUI.text(g, "^ MORE", x + w - 8 - BattleUI.textW("^ MORE", 1), y + 4,
                1, BattleUI.GREY);
        if (top + visible < total) BattleUI.text(g, "v MORE", x + w - 8 - BattleUI.textW("v MORE", 1),
                y + h - 12, 1, BattleUI.GREY);
    }

    // "Sharpness II, Unbreaking I" in enum order, or "NO ENCHANTS"
    private static String enchantSummary(Item item) {
        Objects.requireNonNull(item, "item cannot be null");
        if (item.getEnchants().isEmpty()) return "NO ENCHANTS";
        StringBuilder text = new StringBuilder();
        item.getEnchants().asMap().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<EnchantmentType, Integer> entry) -> entry.getKey().ordinal()))
                .forEach((Map.Entry<EnchantmentType, Integer> entry) -> {
                    if (!text.isEmpty()) text.append(", ");
                    text.append(entry.getKey().getDisplayName()).append(' ').append(Enchantment.toRoman(entry.getValue()));
                });
        return text.toString();
    }

    // "MELEE", or "MELEE, RANGED WEAPON, SHIELD, ARMOR": the slots a book of this type goes on, in slot order
    private static String slotNames(EnchantmentType type) {
        Objects.requireNonNull(type, "enchant type cannot be null");
        return String.join(", ", type.getSlots().stream()
                .sorted(Comparator.comparingInt((Inventory.EquipSlot slot) -> slot.ordinal()))
                .map((Inventory.EquipSlot slot) -> slot.name().replace('_', ' ')).toList());
    }

    // cuts text that is too wide for a row and marks the cut with "..."
    private static String fit(String text, int maxChars) {
        Objects.requireNonNull(text, "text cannot be null");
        if (text.length() <= maxChars) return text;
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    // an enchantment effect is a fraction (0.06 -> "+6%")
    private static String formatPercent(double fraction) {
        String text = String.format(Locale.ROOT, "%.1f", fraction * 100.0);
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        return "+" + text + "%";
    }

    private static void drawSprite(GraphicsHandler g, Item item, int x, int y, int size) {
        Objects.requireNonNull(item, "item cannot be null");
        if (item.getItemLook() == null) return;
        BufferedImage sprite = item.getItemLook().getSprite(0, 0);
        g.drawImage(sprite, x, y, size, size);
    }
}
