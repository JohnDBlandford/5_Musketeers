package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;
import Engine.Screen;
import Item.Item;
import Screens.InventoryScreen;
import inventory.Inventory;

import java.awt.*;

// The small strip under the enemy's plate:  M S R H
// M, S, R light up when a melee weapon, shield or ranged weapon is equipped, H lights up while the heal is still available.
public final class InventoryDisplayScreen extends Screen {
    private final Inventory inventory;
    private static final Inventory.EquipSlot[] ORDER = { Inventory.EquipSlot.MELEE, Inventory.EquipSlot.SHIELD,
            Inventory.EquipSlot.RANGED_WEAPON, Inventory.EquipSlot.ARMOR };
    private static final int SLOT = 48, GAP = 6;
    private static final Color SLOT_FILL = new Color(35, 35, 35);
    private static final Color SLOT_BORDER = new Color(90, 90, 90);

    public InventoryDisplayScreen(Inventory inventory, PlayBattleScreen battle) {
        this.inventory = inventory;
    }

    @Override
    public void initialize() {}

    @Override
    public void update() {}

    @Override
    public void draw(GraphicsHandler gh) {
        int y = BattleUI.barTop() + BattleUI.boxH() + 8;
        for (int i = 0; i < ORDER.length; i++) {
            int x = BattleUI.boxX() + 4 + i * (SLOT + GAP);
            Item item = inventory.getEquippedItem(ORDER[i]);
            gh.drawFilledRectangleWithBorder(x, y, SLOT, SLOT, SLOT_FILL, SLOT_BORDER, 2);
            if (item == null) {
                int scale = 2;
                BattleUI.textCenter(gh, ORDER[i].name().substring(0, 1), x + SLOT / 2,
                        y + (SLOT - BattleUI.font().getCapHeight(scale)) / 2, scale, Color.gray);
                continue;
            }
            gh.drawFilledRectangle(x + 4, y + 4, SLOT - 8, SLOT - 8, InventoryScreen.rarityColor(item));
            if (item.getItemLook() != null)
                gh.drawImage(item.getItemLook().getSprite(0, 0), x + 4, y + 4, SLOT - 8, SLOT - 8);
        }
    }
}