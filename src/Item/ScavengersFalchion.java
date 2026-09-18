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
}
