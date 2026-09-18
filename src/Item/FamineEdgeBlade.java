package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeBlade extends Sword {
    FamineEdgeBlade(Sprite itemLook,
                    Map<ItemAbilities, Item> itemAbilities,
                    Map<ItemBuffs, Item> itemBuffs) {
        super("FamineEdgeBlade", itemLook, ItemRarity.EPIC,
                itemAbilities, itemBuffs, 60,
                ItemBuffs.CRIT_CHANCE, ItemBuffs.ATK_BOOST,
                ItemBuffs.NONE);
    }
}
