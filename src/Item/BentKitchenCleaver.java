package Item;

import GameObject.Sprite;

import java.util.Map;

public final class BentKitchenCleaver extends Sword {
    BentKitchenCleaver(Sprite itemLook,
                       Map<ItemAbilities, Item> itemAbilities,
                       Map<ItemBuffs, Item> itemBuffs) {
        super("BentKitchenCleaver", itemLook,
                ItemRarity.COMMON, itemAbilities, itemBuffs,
                35, ItemBuffs.NONE, ItemBuffs.NONE,
                ItemBuffs.NONE);

    }
}
