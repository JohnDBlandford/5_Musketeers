package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeAegis extends Shield {
    FamineEdgeAegis(Sprite itemLook,
                    Map<ItemAbilities, Item> itemAbilities,
                    Map<ItemBuffs, Item> itemBuffs) {
        super("FamineEdgeAegis", itemLook, ItemRarity.EPIC,
                itemAbilities, itemBuffs, 0.20,
                0.45, ItemBuffs.BONUS_DEF,
                ItemBuffs.DEF_BOOST, ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case BONUS_DEF -> 0.16;
            case DEF_BOOST -> 0.06;
            default -> 0.0;
        };
    }
}
