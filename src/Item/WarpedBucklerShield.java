package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WarpedBucklerShield extends Shield {
    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.BULWARK_STANCE, AbilityType.BUFF,
            ItemAbilities.SHIELD_BASH, AbilityType.CONTROL,
            ItemAbilities.SHIELD_THROW, AbilityType.CONTROL,
            ItemAbilities.RETALIATE, AbilityType.COUNTER
    );

    WarpedBucklerShield(Sprite itemLook) {
        super("WarpedBucklerShield", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 0.10, 0.28);
    }
}
