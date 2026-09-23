package Item;

import GameObject.Sprite;

import java.util.Map;

public final class PatchedLeatherArmor extends Armor {
    PatchedLeatherArmor(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("PatchedLeatherArmor", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 10);
    }
}
