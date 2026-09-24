package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class SplinteredSling extends RangedWeapon {
    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.WINDBORNE_ARROW, AbilityType.BUFF,
            ItemAbilities.PIERCING_SHOT, AbilityType.BURST,
            ItemAbilities.VOLLEY, AbilityType.MULTI_HIT,
            ItemAbilities.HUNTERS_MARK, AbilityType.DEBUFF
    );

    SplinteredSling(SpriteSheet itemLook) {
        super("SplinteredSling", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 30);
    }
}
