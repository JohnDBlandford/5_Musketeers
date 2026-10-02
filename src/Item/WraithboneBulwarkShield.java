package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class WraithboneBulwarkShield extends Shield {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.22,
            ItemBuffs.DEF_BOOST, 0.09,
            ItemBuffs.THORNS, 0.12
    );

    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.BULWARK_STANCE, 0.00),
            AbilityType.CONTROL, Map.of(ItemAbilities.SHIELD_BASH, 0.00),
            AbilityType.FINISHER, Map.of(ItemAbilities.SHIELD_THROW, 0.0),
            AbilityType.COUNTER, Map.of(ItemAbilities.RETALIATE, 0.00)
    );

    WraithboneBulwarkShield(SpriteSheet itemLook) {
        super("WraithboneBulwarkShield", itemLook, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 0.25, 0.55);
    }
}
