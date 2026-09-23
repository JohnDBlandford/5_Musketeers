package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneRepeater extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.10,
            ItemBuffs.CRIT_CHANCE, 0.08,
            ItemBuffs.PIERCING, 0.08
    );
    WraithboneRepeater(Sprite itemLooks, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("WraithboneRepeater", itemLooks, ItemRarity.LEGENDARY, itemAbilities, BUFFS, 75);
    }

    @Override
    public int getRangedDmg() { return super.getRangedDmg(); }
}
