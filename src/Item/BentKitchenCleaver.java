package Item;

import GameObject.Sprite;

import java.util.Map;

public final class BentKitchenCleaver extends Sword {
    BentKitchenCleaver(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("BentKitchenCleaver", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 35);
    }
}
