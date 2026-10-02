package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class WraithboneMailArmor extends Armor {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 0.85,
            ItemBuffs.SPD_BOOST, 0.09,
            ItemBuffs.DMG_REDUCTION, 0.05
    );

    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.HEAL, Map.of(ItemAbilities.SECOND_WIND, 0.18),
            AbilityType.SPD_BOOST, Map.of(ItemAbilities.LIGHT_FOOTING, 0.12),
            AbilityType.DMG_REDUCTION, Map.of(ItemAbilities.HARDEN, 0.16),
            AbilityType.BONUS_DEF, Map.of(ItemAbilities.IRON_STANCE, 0.22),
            AbilityType.HEALTH_RESTORE, Map.of(ItemAbilities.GRIT, 0.15)
    );

    WraithboneMailArmor(SpriteSheet itemLook) {
        super("WraithboneMailArmor", itemLook, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 28);
    }
}
