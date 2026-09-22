package Item;

import GameObject.Sprite;

import java.util.Map;

public final class ScavengersCrossBow extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.06
    );
    ScavengersCrossBow(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities) {
        super("ScavengersCrossBow", itemLook, ItemRarity.UNCOMMON, itemAbilities, BUFFS, 40);
    }
}
