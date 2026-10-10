package Item.RangedWeapon;

import GameObject.SpriteSheet;

import java.util.Map;

public final class FamineEdgeLongbow extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.08,
            ItemBuffs.CRIT_CHANCE, 0.06
    );

    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.WINDBORNE_ARROW, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.PIERCING_SHOT, 0.00),
            AbilityType.MULTI_HIT, Map.of(ItemAbilities.VOLLEY, 0.00),
            AbilityType.DEBUFF, Map.of(ItemAbilities.HUNTERS_MARK, 0.00)
    );

    public FamineEdgeLongbow(SpriteSheet itemLook) {
        super("FamineEdgeLongbow", itemLook, ItemRarity.EPIC, ABILITIES, BUFFS, 55);
    }
}
