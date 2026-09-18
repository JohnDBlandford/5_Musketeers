package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneSaber extends Sword {
    WraithboneSaber(Sprite itemLook,
                    Map<ItemAbilities, Item> itemAbilities,
                    Map<ItemBuffs, Item> itemBuffs) {
        super("WraithboneSaber", itemLook,
                ItemRarity.LEGENDARY, itemAbilities, itemBuffs,
                80, ItemBuffs.CRIT_CHANCE,
                ItemBuffs.ATK_BOOST, ItemBuffs.CRIT_DMG);
    }
}
