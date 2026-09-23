package Item;

import GameObject.Sprite;

import java.util.Map;

public final class BentKitchenCleaver extends Sword {
    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.GALE_SLASH, AbilityType.BUFF,
            ItemAbilities.HEAVY_CLEAVE, AbilityType.BURST,
            ItemAbilities.BLOODFANG_STRIKE, AbilityType.SUSTAIN,
            ItemAbilities.RECKONING_BLOW, AbilityType.FINISHER
    );

    BentKitchenCleaver(Sprite itemLook) {
        super("BentKitchenCleaver", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 35);
    }
}
