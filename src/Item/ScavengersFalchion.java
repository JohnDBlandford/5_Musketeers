package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class ScavengersFalchion extends Sword {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.06
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.GALE_SLASH, AbilityType.BUFF,
            ItemAbilities.HEAVY_CLEAVE, AbilityType.BURST,
            ItemAbilities.BLOODFANG_STRIKE, AbilityType.SUSTAIN,
            ItemAbilities.RECKONING_BLOW, AbilityType.FINISHER
    );

    ScavengersFalchion(SpriteSheet itemLook) {
        super("ScavengersFalchion", itemLook, ItemRarity.UNCOMMON, ABILITIES, BUFFS, 45);
    }
}
