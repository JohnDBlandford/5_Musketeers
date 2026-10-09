package Item.RangedWeapon;

import GameObject.SpriteSheet;

import java.util.Map;

public final class ScavengersCrossBow extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.06
    );

    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.WINDBORNE_ARROW, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.PIERCING_SHOT, 0.00),
            AbilityType.MULTI_HIT, Map.of(ItemAbilities.VOLLEY, 0.0),
            AbilityType.DEBUFF, Map.of(ItemAbilities.HUNTERS_MARK, 0.00)
    );

    public ScavengersCrossBow(SpriteSheet itemLook) {
        super("ScavengersCrossBow", itemLook, ItemRarity.UNCOMMON, ABILITIES, BUFFS, 40);
    }
}
