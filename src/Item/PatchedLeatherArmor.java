package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class PatchedLeatherArmor extends Armor {
    public PatchedLeatherArmor(SpriteSheet itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("PatchedLeatherArmor", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 10);
    }
}
