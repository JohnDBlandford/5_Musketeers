package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class ScavengersHideArmor extends Armor {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 0.40
    );
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.HEAL, Map.of(ItemAbilities.SECOND_WIND, 0.08),
            AbilityType.SPD_BOOST, Map.of(ItemAbilities.LIGHT_FOOTING, 0.05),
            AbilityType.DMG_REDUCTION, Map.of(ItemAbilities.HARDEN, 0.08),
            AbilityType.BONUS_DEF, Map.of(ItemAbilities.IRON_STANCE, 0.09)
    );

    ScavengersHideArmor(SpriteSheet itemLook) {
        super("ScavengersHideArmor", itemLook, ItemRarity.UNCOMMON, ABILITIES, BUFFS, 14);
    }
}
