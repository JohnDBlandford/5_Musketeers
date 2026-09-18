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

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case CRIT_CHANCE -> 0.08;
            case ATK_BOOST -> 0.10;
            default -> 0.0;
        };
    }
}
