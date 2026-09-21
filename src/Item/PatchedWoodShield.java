package Item;

import GameObject.Sprite;

import java.util.Map;

public final class PatchedWoodShield extends Shield {
    PatchedWoodShield(Sprite itemLooks,
                      Map<ItemAbilities, Item> itemAbilities,
                      Map<ItemBuffs, Item> itemBuffs) {
        super("PatchedWoodShield", itemLooks,
                ItemRarity.COMMON, itemAbilities, itemBuffs,
                0.12, 0.30, ItemBuffs.NONE,
                ItemBuffs.NONE, ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }
}
