package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WarpedBucklerShield extends Shield {
    WarpedBucklerShield(Sprite itemLook,
                        Map<ItemAbilities, Item> itemAbilities,
                        Map<ItemBuffs, Item> itemBuffs) {
        super("WarpedBucklerShield", itemLook,
                ItemRarity.COMMON, itemAbilities, itemBuffs,
                0.10, 0.28,
                ItemBuffs.NONE, ItemBuffs.NONE,
                ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }
}
