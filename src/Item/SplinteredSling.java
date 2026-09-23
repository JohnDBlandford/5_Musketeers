package Item;

import GameObject.Sprite;

import java.util.Map;

public final class SplinteredSling extends RangedWeapon {
    SplinteredSling(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("SplinteredSling", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 30);
    }
}
