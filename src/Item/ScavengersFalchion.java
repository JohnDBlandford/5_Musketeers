package Item;

import GameObject.Sprite;

import java.util.Map;

public final class ScavengersFalchion extends Sword {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.06
    );
    ScavengersFalchion(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("ScavengersFalchion", itemLook, ItemRarity.UNCOMMON, itemAbilities, BUFFS, 45);
    }
}
