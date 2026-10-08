package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class PatchedLeatherArmor extends Armor {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.HEAL, Map.of(ItemAbilities.SECOND_WIND, 0.05),
            AbilityType.SPD_BOOST, Map.of(ItemAbilities.LIGHT_FOOTING, 0.03),
            AbilityType.DMG_REDUCTION, Map.of(ItemAbilities.HARDEN, 0.05),
            AbilityType.BONUS_DEF, Map.of(ItemAbilities.IRON_STANCE, 0.05)
    );

    PatchedLeatherArmor(SpriteSheet itemLook) {
        super("PatchedLeatherArmor", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 10);
    }
}
