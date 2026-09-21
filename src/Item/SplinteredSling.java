package Item;

import GameObject.Sprite;

import java.util.Map;

public final class SplinteredSling extends RangedWeapon {
    SplinteredSling(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities,
                    Map<ItemBuffs, Item> itemBuffs) {
        super("SplinteredSling", itemLook,
                ItemRarity.COMMON, itemAbilities, itemBuffs,
                30, ItemBuffs.NONE, ItemBuffs.NONE,
                ItemBuffs.NONE, ItemBuffs.NONE, ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }
}
