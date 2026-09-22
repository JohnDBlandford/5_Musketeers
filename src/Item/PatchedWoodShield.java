package Item;

import GameObject.Sprite;

import java.util.Map;

public final class PatchedWoodShield extends Shield {
    PatchedWoodShield(Sprite itemLooks, Map<ItemAbilities, Item> itemAbilities) {
        super("PatchedWoodShield", itemLooks, ItemRarity.COMMON, itemAbilities, Map.of(), 0.12, 0.30);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }
}
