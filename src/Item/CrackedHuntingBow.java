package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class CrackedHuntingBow extends RangedWeapon {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.WINDBORNE_ARROW, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.PIERCING_SHOT, 0.00),
            AbilityType.MULTI_HIT, Map.of(ItemAbilities.VOLLEY, 0.0),
            AbilityType.DEBUFF, Map.of(ItemAbilities.HUNTERS_MARK, 0.00)
    );

    CrackedHuntingBow(SpriteSheet itemLook) {
        super("CrackedHuntingBow", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 25);
    }
}
