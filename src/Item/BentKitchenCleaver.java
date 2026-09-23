package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class BentKitchenCleaver extends Sword {
    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.GALE_SLASH, AbilityType.BUFF,
            ItemAbilities.HEAVY_CLEAVE, AbilityType.BURST,
            ItemAbilities.BLOODFANG_STRIKE, AbilityType.SUSTAIN,
            ItemAbilities.RECKONING_BLOW, AbilityType.FINISHER
    );

    public BentKitchenCleaver(SpriteSheet itemLook) {
        super("BentKitchenCleaver", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 35);
    }
}
