package Item.Sword;

import GameObject.SpriteSheet;

import java.util.Map;

public final class BentKitchenCleaver extends Sword {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.GALE_SLASH, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.HEAVY_CLEAVE, 0.00),
            AbilityType.SUSTAIN, Map.of(ItemAbilities.BLOODFANG_STRIKE, 0.0),
            AbilityType.FINISHER, Map.of(ItemAbilities.RECKONING_BLOW, 0.00)
    );

    public BentKitchenCleaver(SpriteSheet itemLook) {
        super("BentKitchenCleaver", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 35);
    }
}
