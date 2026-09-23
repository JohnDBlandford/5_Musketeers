package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class FamineEdgeLongbow extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.08,
            ItemBuffs.CRIT_CHANCE, 0.06
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.WINDBORNE_ARROW, AbilityType.BUFF,
            ItemAbilities.PIERCING_SHOT, AbilityType.BURST,
            ItemAbilities.VOLLEY, AbilityType.MULTI_HIT,
            ItemAbilities.HUNTERS_MARK, AbilityType.DEBUFF
    );

    FamineEdgeLongbow(SpriteSheet itemLook) {
        super("FamineEdgeLongbow", itemLook, ItemRarity.EPIC, ABILITIES, BUFFS, 55);
    }
}
