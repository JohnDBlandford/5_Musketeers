package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class ScavengersRoundShield extends Shield {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.10
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.BULWARK_STANCE, AbilityType.BUFF,
            ItemAbilities.SHIELD_BASH, AbilityType.CONTROL,
            ItemAbilities.SHIELD_THROW, AbilityType.CONTROL,
            ItemAbilities.RETALIATE, AbilityType.COUNTER
    );

    ScavengersRoundShield(SpriteSheet itemLook) {
        super("ScavengersRoundShield", itemLook, ItemRarity.UNCOMMON, ABILITIES, BUFFS, 0.16, 0.38);
    }
}
