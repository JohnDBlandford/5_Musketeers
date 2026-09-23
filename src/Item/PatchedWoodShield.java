package Item;

import GameObject.Sprite;

import java.util.Map;

public final class PatchedWoodShield extends Shield {
    PatchedWoodShield(Sprite itemLooks, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("PatchedWoodShield", itemLooks, ItemRarity.COMMON, itemAbilities, Map.of(), 0.12, 0.30);
    }
}
