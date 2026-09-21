package Item;

import GameObject.Sprite;

import java.util.Map;

public final class ScavengersCrossBow extends RangedWeapon {
    ScavengersCrossBow(Sprite itemLook,
                       Map<ItemAbilities, Item> itemAbilities,
                       Map<ItemBuffs, Item> itemBuffs) {
        super("ScavengersCrossBow", itemLook,
                ItemRarity.UNCOMMON, itemAbilities, itemBuffs,
                40, ItemBuffs.NONE, ItemBuffs.NONE,
                ItemBuffs.ACCURACY, ItemBuffs.NONE, ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case ACCURACY -> 0.06;
            default -> 0.0;
        };
    }
}
