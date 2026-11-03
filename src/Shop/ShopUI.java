package Shop;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Item.Item;

import java.awt.Color;

// Drawing pieces that MerchantScreen and BlacksmithScreen share, so the two shop windows look alike and neither depends on the
// other. Only things both screens really draw the same way live here: the window colours, the rarity colours and the one-line
// text helper.
public final class ShopUI {
    private ShopUI() {}

    public static final Color DIM = new Color(0, 0, 0, 140);
    public static final Color PANEL_FILL = new Color(60, 60, 60);
    public static final Color SLOT_FILL = new Color(35, 35, 35);
    public static final Color SLOT_BORDER = new Color(90, 90, 90);
    public static final Color GOOD = new Color(110, 220, 120);
    public static final Color BAD = new Color(235, 90, 80);

    public static Color rarityColor(Item.ItemRarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> new Color(80, 170, 80);
            case EPIC -> new Color(150, 80, 200);
            case LEGENDARY -> new Color(230, 150, 40);
            default -> new Color(130, 130, 130);
        };
    }

    // draws one line of text (scale 2, dropping to scale 1 if it is too wide) and returns the y of the next line; stops drawing past `bottom`
    public static int line(GraphicsHandler g, String text, int x, int y, int width, Color color, int bottom) {
        int scale = BattleUI.textW(text, 2) <= width ? 2 : 1;
        int next = y + BattleUI.font().getCapHeight(2) + 8;
        if (y + BattleUI.font().getCapHeight(scale) <= bottom) BattleUI.text(g, text, x, y, scale, color);
        return next;
    }
}
