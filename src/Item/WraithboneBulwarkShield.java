package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneBulwarkShield extends Shield {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.22,
            ItemBuffs.DEF_BOOST, 0.09,
            ItemBuffs.THORNS, 0.12
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.BULWARK_STANCE, AbilityType.BUFF,
            ItemAbilities.SHIELD_BASH, AbilityType.CONTROL,
            ItemAbilities.SHIELD_THROW, AbilityType.CONTROL,
            ItemAbilities.RETALIATE, AbilityType.COUNTER
    );

    WraithboneBulwarkShield(Sprite itemLook) {
        super("WraithboneBulwarkShield", itemLook, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 0.25, 0.55);
    }
}
