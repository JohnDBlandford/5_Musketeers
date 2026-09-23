package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneRepeater extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.10,
            ItemBuffs.CRIT_CHANCE, 0.08,
            ItemBuffs.PIERCING, 0.08
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.WINDBORNE_ARROW, AbilityType.BUFF,
            ItemAbilities.PIERCING_SHOT, AbilityType.BURST,
            ItemAbilities.VOLLEY, AbilityType.MULTI_HIT,
            ItemAbilities.HUNTERS_MARK, AbilityType.DEBUFF
    );

    WraithboneRepeater(Sprite itemLooks) {
        super("WraithboneRepeater", itemLooks, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 75);
    }
}
