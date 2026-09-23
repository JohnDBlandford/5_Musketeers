package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class ScavengersCrossBow extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.06
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.WINDBORNE_ARROW, AbilityType.BUFF,
            ItemAbilities.PIERCING_SHOT, AbilityType.BURST,
            ItemAbilities.VOLLEY, AbilityType.MULTI_HIT,
            ItemAbilities.HUNTERS_MARK, AbilityType.DEBUFF
    );

    public ScavengersCrossBow(SpriteSheet itemLook) {
        super("ScavengersCrossBow", itemLook, ItemRarity.UNCOMMON, ABILITIES, BUFFS, 40);
    }
}
