package Item;

import java.util.Map;

import GameObject.SpriteSheet;

public final class RustedShortSword extends Sword {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.GALE_SLASH, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.HEAVY_CLEAVE, 0.00),
            AbilityType.SUSTAIN, Map.of(ItemAbilities.BLOODFANG_STRIKE, 0.0),
            AbilityType.FINISHER, Map.of(ItemAbilities.RECKONING_BLOW, 0.00)
    );

    RustedShortSword(SpriteSheet itemLook) {
        super("RustedShortSword", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 30);
    }
}
