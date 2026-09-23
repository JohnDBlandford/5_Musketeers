package Item;

import java.util.Map;

import GameObject.Sprite;

public final class RustedShortSword extends Sword {
    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.GALE_SLASH, AbilityType.BUFF,
            ItemAbilities.HEAVY_CLEAVE, AbilityType.BURST,
            ItemAbilities.BLOODFANG_STRIKE, AbilityType.SUSTAIN,
            ItemAbilities.RECKONING_BLOW, AbilityType.FINISHER
    );

    RustedShortSword(Sprite itemLook) {
        super("RustedShortSword", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 30);
    }
}
