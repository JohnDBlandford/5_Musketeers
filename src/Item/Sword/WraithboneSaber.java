package Item.Sword;

import GameObject.SpriteSheet;

import java.util.Map;

public final class WraithboneSaber extends Sword {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.10,
            ItemBuffs.ATK_BOOST, 0.14,
            ItemBuffs.CRIT_DMG, 0.25
    );

    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.GALE_SLASH, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.HEAVY_CLEAVE, 0.00),
            AbilityType.SUSTAIN, Map.of(ItemAbilities.BLOODFANG_STRIKE, 0.0),
            AbilityType.FINISHER, Map.of(ItemAbilities.RECKONING_BLOW, 0.00)
    );

    public WraithboneSaber(SpriteSheet itemLook) {
        super("WraithboneSaber", itemLook, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 80);
    }
}
