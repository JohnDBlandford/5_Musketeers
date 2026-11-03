package Shop.Merchant;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.Screen;
import Game.PlayerData;
import Item.Consumable.Consumable;
import Item.Item;
import Item.ItemEntry;
import Item.Registry.ItemRegistry;
import Shop.Merchant.IMerchant.SlotView;
import Shop.PurchaseResult;
import Shop.ShopController;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// The merchant's shop window. UI only: every decision (selection, quantity stepper, buying, the status message) lives in
// MerchantViewModel; this class maps keys to it and draws what it reports. Shown by ShopController, which restocks and
// calls onOpen(world) first.
//   LEFT / RIGHT: previous / next item     UP / DOWN: quantity     ENTER: buy     ESC: close
// Food and water have no sprites yet, so they get a boxed "F" / "W" placeholder
public final class MerchantScreen extends Screen implements ShopController.Panel {
    private static final int COLS = 5;
    private static final int SLOT = 56;
    private static final int GAP = 8;
    private static final int PITCH_X = SLOT + GAP;
    private static final int PITCH_Y = SLOT + 30;   // room for the price line under each slot

    private static final Color DIM = new Color(0, 0, 0, 140);
    private static final Color PANEL_FILL = new Color(60, 60, 60);
    private static final Color SLOT_FILL = new Color(35, 35, 35);
    private static final Color SLOT_BORDER = new Color(90, 90, 90);
    private static final Color SOLD_OUT_SHADE = new Color(0, 0, 0, 170);
    private static final Color GOOD = new Color(110, 220, 120);
    private static final Color BAD = new Color(235, 90, 80);

    private static final Key[] INPUT_KEYS = {Key.ESC, Key.ENTER, Key.LEFT, Key.RIGHT, Key.UP, Key.DOWN};

    private final MerchantViewModel viewModel;
    private final Runnable closeRequest;   // PlayLevelScreen passes () -> shopController.close()
    private final KeyLocker keyLocker = new KeyLocker();
    private final Map<String, Object> details = new HashMap<>();   // registry id -> Item or Consumable, built once per opening so draw() stays cheap
    private PurchaseResult lastResult = null;

    public MerchantScreen(MerchantViewModel viewModel, Runnable closeRequest) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.closeRequest = Objects.requireNonNull(closeRequest, "closeRequest");
    }

    @Override
    public void initialize() {}

    @Override
    public void onOpen(int world) {
        viewModel.open(world);
        details.clear();
        lastResult = null;
        // keys already held when the shop opens (the ENTER that dismissed the greeting) must not count as presses
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
                case LEFT -> viewModel.selectPrevious();
                case RIGHT -> viewModel.selectNext();
                case UP -> viewModel.increaseQuantity();
                case DOWN -> viewModel.decreaseQuantity();
                case ENTER -> lastResult = viewModel.buy();
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
        int gridX = panelX + 30;
        int gridY = panelY + 80;
        int gridW = COLS * PITCH_X - GAP;
        int detailX = gridX + gridW + 30;
        int detailW = panelX + panelW - 30 - detailX;
        int detailH = panelY + panelH - 130 - gridY;

        g.drawFilledRectangle(0, 0, screenW, screenH, DIM);
        g.drawFilledRectangleWithBorder(panelX, panelY, panelW, panelH, PANEL_FILL, Color.black, 3);

        BattleUI.shadowText(g, "MERCHANT", panelX + 30, panelY + 24, 3, BattleUI.WHITE);
        BattleUI.shadowText(g, "GOLD " + PlayerData.getCurrency(), panelX + panelW - 30 - BattleUI.textW("GOLD " +
                        PlayerData.getCurrency(), 2), panelY + 28, 2, BattleUI.GOLD);

        List<SlotView> slots = viewModel.getSlots();
        int selected = viewModel.getSelectedIndex();
        for (int i = 0; i < slots.size(); i++) {
            drawSlot(g, slots.get(i), gridX + (i % COLS) * PITCH_X, gridY + (i / COLS) * PITCH_Y);
        }
        if (!slots.isEmpty()) {
            g.drawRectangle(gridX + (selected % COLS) * PITCH_X - 3, gridY + (selected / COLS) * PITCH_Y - 3, SLOT + 6,
                    SLOT + 6, Color.white, 3);
        } else {
            BattleUI.text(g, "NOTHING FOR SALE", gridX, gridY + 10, 2, BattleUI.GREY);
        }

        drawDetails(g, viewModel.getSelected(), detailX, gridY, detailW, detailH);
        drawPurchaseBar(g, panelX, panelY, panelW, panelH);
    }

    private void drawSlot(GraphicsHandler g, SlotView slot, int x, int y) {
        g.drawFilledRectangleWithBorder(x, y, SLOT, SLOT, SLOT_FILL, SLOT_BORDER, 2);
        g.drawFilledRectangle(x + 4, y + 4, SLOT - 8, SLOT - 8, rarityColor(slot.rarity()));
        if (slot.kind() == ItemEntry.ItemKind.GEAR) {
            Item item = gear(slot.id());
            if (item != null && item.getItemLook() != null) {
                BufferedImage sprite = item.getItemLook().getSprite(0, 0);
                g.drawImage(sprite, x + 4, y + 4, SLOT - 8, SLOT - 8);
            }
        } else { drawPlaceholderLetter(g, consumableLetter(consumable(slot.id())), x, y); }
        if (slot.isSoldOut()) {
            g.drawFilledRectangle(x + 2, y + 2, SLOT - 4, SLOT - 4, SOLD_OUT_SHADE);
            BattleUI.textCenter(g, "SOLD", x + SLOT / 2, y + (SLOT - BattleUI.font().getCapHeight(1)) / 2,
                    1, BattleUI.WHITE);
        } else if (!slot.unlimited()) {
            BattleUI.shadowText(g, "x" + slot.remainingStock(), x + 4, y + 4, 1, BattleUI.WHITE);
        }
        boolean affordable = PlayerData.getCurrency() >= slot.unitPrice();
        BattleUI.textCenter(g, String.valueOf(slot.unitPrice()), x + SLOT / 2, y + SLOT + 8, 1, affordable
                ? BattleUI.GOLD : BAD);
    }

    // PLACEHOLDER: food and water have no sprites yet
    private void drawPlaceholderLetter(GraphicsHandler g, String letter, int x, int y) {
        g.drawFilledRectangleWithBorder(x + 10, y + 10, SLOT - 20, SLOT - 20, SLOT_FILL, Color.black,
                2);
        int scale = 3;
        BattleUI.textCenter(g, letter, x + SLOT / 2, y + (SLOT - BattleUI.font().getCapHeight(scale)) / 2, scale,
                BattleUI.WHITE);
    }

    private void drawDetails(GraphicsHandler g, SlotView slot, int x, int y, int w, int h) {
        g.drawFilledRectangleWithBorder(x, y, w, h, SLOT_FILL, SLOT_BORDER, 2);
        if (slot == null) return;
        int tx = x + 12;
        int width = w - 24;
        int bottom = y + h - 10;
        int ty = y + 12;

        int nameChars = Math.max(1, width / (6 * 2));   // every letter is 6 px wide at scale 1
        for (String line : BattleUI.wrap(BattleUI.prettyName(slot.id()), nameChars)) {
            BattleUI.text(g, line, tx, ty, 2, rarityColor(slot.rarity()));
            ty += BattleUI.font().getCapHeight(2) + 8;
        }
        ty += 4;
        ty = line(g, slot.rarity().name(), "", tx, ty, width, Color.lightGray, bottom);
        ty = line(g, "Price", String.valueOf(slot.unitPrice()), tx, ty, width, BattleUI.GOLD, bottom);
        ty = line(g, "Stock", slot.unlimited() ? "UNLIMITED" : String.valueOf(slot.remainingStock()), tx, ty, width, Color.white,
                bottom);
        ty += 6;
        g.drawFilledRectangle(tx, ty, width, 2, SLOT_BORDER);
        ty += 10;

        if (slot.kind() == ItemEntry.ItemKind.GEAR) {
            Item item = gear(slot.id());
            if (item == null) return;
            for (Map.Entry<Item.ItemBuffs, Double> entry : item.getScaledBuffs().entrySet()) {
                ty = line(g, entry.getKey().toString().replace('_', ' '), formatBuff(entry.getKey(), entry.getValue()), tx, ty,
                        width, Color.white, bottom);
            }
            ty = line(g, "Attack", String.valueOf(item.getAttack()), tx, ty, width, Color.white, bottom);
            line(g, "Defense", String.valueOf(item.getDefense()), tx, ty, width, Color.white, bottom);
        } else {
            Consumable c = consumable(slot.id());
            if (c == null) return;
            if (c.getHungerRestore() > 0) ty = line(g, "Hunger", "+" + c.getHungerRestore(), tx, ty, width, Color.white,
                    bottom);
            if (c.getHydrationRestore() > 0) ty = line(g, "Hydration", "+" + c.getHydrationRestore(), tx, ty, width,
                    Color.white, bottom);
            if (c.getHealthRestore() > 0) line(g, "Health", "+" + c.getHealthRestore(), tx, ty, width, Color.white, bottom);
        }
    }

    // draws "LABEL  value" (scale 2, dropping to scale 1 if it is too wide) and returns the y of the next line; stops drawing past `bottom`
    private static int line(GraphicsHandler g, String label, String value, int x, int y, int width, Color color, int bottom) {
        String text = value.isEmpty() ? label : label + "  " + value;
        int scale = BattleUI.textW(text, 2) <= width ? 2 : 1;
        int next = y + BattleUI.font().getCapHeight(2) + 8;
        if (y + BattleUI.font().getCapHeight(scale) <= bottom) BattleUI.text(g, text, x, y, scale, color);
        return next;
    }

    private void drawPurchaseBar(GraphicsHandler g, int panelX, int panelY, int panelW, int panelH) {
        int x = panelX + 30;
        int y = panelY + panelH - 118;
        SlotView selected = viewModel.getSelected();
        if (selected != null) {
            int qty = viewModel.getQuantity();
            int total = viewModel.getTotalPrice();
            boolean affordable = PlayerData.getCurrency() >= total;
            BattleUI.text(g, "QTY", x, y, 2, BattleUI.WHITE);
            BattleUI.text(g, "< " + qty + " >", x + 70, y, 2, BattleUI.WHITE);
            BattleUI.text(g, "TOTAL " + total, x + 190, y, 2, affordable ? BattleUI.GOLD : BAD);
        }
        String message = viewModel.getMessage();
        if (!message.isEmpty()) {
            BattleUI.text(g, message, x, y + 34, 2, lastResult == PurchaseResult.SUCCESS ? GOOD : BAD);
        }
        // key hints, two lines at scale 2 so they are easy to read (one line at this size would be wider than the panel)
        BattleUI.shadowText(g, "LEFT/RIGHT: ITEM   UP/DOWN: QUANTITY", x, panelY + panelH - 56, 2, BattleUI.WHITE);
        BattleUI.shadowText(g, "ENTER: BUY   ESC: CLOSE", x, panelY + panelH - 34, 2, BattleUI.WHITE);
    }

    // helpers
    private Item gear(String id) {
        return (Item) details.computeIfAbsent(id, ItemRegistry::createItem);
    }

    private Consumable consumable(String id) {
        return (Consumable) details.computeIfAbsent(id, ItemRegistry::createConsumable);
    }

    // PLACEHOLDER rule: hydration means water, anything else is food (same split MerchantStock uses for its two guaranteed slots)
    static String consumableLetter(Consumable c) {
        return c != null && c.getHydrationRestore() > 0 ? "W" : "F";
    }

    private static Color rarityColor(Item.ItemRarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> new Color(80, 170, 80);
            case EPIC -> new Color(150, 80, 200);
            case LEGENDARY -> new Color(230, 150, 40);
            default -> new Color(130, 130, 130);
        };
    }

    // flat buffs are stored as value / 100, everything else is a fraction shown as a percentage (same rule as InventoryScreen)
    private static String formatBuff(Item.ItemBuffs buff, double value) {
        if (Item.isFlatBuff(buff)) return "+" + Math.round(value * 100.0);
        String text = String.format(Locale.ROOT, "%.1f", value * 100.0);
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        return text + "%";
    }
}
