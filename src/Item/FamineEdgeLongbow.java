package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeLongbow extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.08,
            ItemBuffs.CRIT_CHANCE, 0.06
    );
    FamineEdgeLongbow(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("FamineEdgeLongbow", itemLook, ItemRarity.EPIC, itemAbilities, BUFFS, 55);
    }

    @Override
    public int getRangedDmg() { return super.getRangedDmg(); }
}
