package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeAegis extends Shield {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.16,
            ItemBuffs.DEF_BOOST, 0.06
    );
    FamineEdgeAegis(Sprite itemLook,
                    Map<ItemAbilities, Item> itemAbilities) {
        super("FamineEdgeAegis", itemLook, ItemRarity.EPIC,
                itemAbilities, BUFFS, 0.20, 0.45);
    }
}
