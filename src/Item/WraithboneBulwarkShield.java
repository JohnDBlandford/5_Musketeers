package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneBulwarkShield extends Shield {
    WraithboneBulwarkShield(Sprite itemLook,
                            Map<ItemAbilities, Item> itemAbilities,
                            Map<ItemBuffs, Item> itemBuffs) {
        super("WraithboneBulwarkShield", itemLook,
                ItemRarity.LEGENDARY, itemAbilities, itemBuffs,
                0.25, 0.55,
                ItemBuffs.BONUS_DEF, ItemBuffs.DEF_BOOST,
                ItemBuffs.THORNS);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case BONUS_DEF -> 0.22;
            case DEF_BOOST -> 0.09;
            case THORNS -> 0.12;
            default -> 0.0;
        };
    }
}
