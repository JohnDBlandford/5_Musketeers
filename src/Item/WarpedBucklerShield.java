package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class WarpedBucklerShield extends Shield {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.BULWARK_STANCE, 0.00),
            AbilityType.CONTROL, Map.of(ItemAbilities.SHIELD_BASH, 0.00),
            AbilityType.FINISHER, Map.of(ItemAbilities.SHIELD_THROW, 0.0),
            AbilityType.COUNTER, Map.of(ItemAbilities.RETALIATE, 0.00)
    );

    WarpedBucklerShield(SpriteSheet itemLook) {
        super("WarpedBucklerShield", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 0.10, 0.28);
    }
}
