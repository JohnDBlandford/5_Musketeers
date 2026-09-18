package Item;

import GameObject.Sprite;

import java.util.Map;

public final class ScavengersFalchion extends Sword {
    ScavengersFalchion(Sprite itemLook,
                       Map<ItemAbilities, Item> itemAbilities,
                       Map<ItemBuffs, Item> itemBuffs) {
        super("ScavengersFalchion", itemLook,
                ItemRarity.UNCOMMON, itemAbilities, itemBuffs,
                45, ItemBuffs.CRIT_CHANCE, ItemBuffs.NONE,
                ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case CRIT_CHANCE -> 0.06;
            default -> 0.0;
        };
    }
}
