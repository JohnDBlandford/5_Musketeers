package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneSaber extends Sword {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.10,
            ItemBuffs.ATK_BOOST, 0.14,
            ItemBuffs.CRIT_DMG, 0.25
    );
    WraithboneSaber(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities) {
        super("WraithboneSaber", itemLook, ItemRarity.LEGENDARY, itemAbilities, BUFFS, 80);
    }
}
