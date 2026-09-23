package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class PatchedWoodShield extends Shield {
    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.BULWARK_STANCE, AbilityType.BUFF,
            ItemAbilities.SHIELD_BASH, AbilityType.CONTROL,
            ItemAbilities.SHIELD_THROW, AbilityType.CONTROL,
            ItemAbilities.RETALIATE, AbilityType.COUNTER
    );

    PatchedWoodShield(SpriteSheet itemLooks) {
        super("PatchedWoodShield", itemLooks, ItemRarity.COMMON, ABILITIES, Map.of(), 0.12, 0.30);
    }
}
