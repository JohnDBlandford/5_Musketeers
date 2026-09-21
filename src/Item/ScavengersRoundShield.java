package Item;

import GameObject.Sprite;

import java.util.Map;

public final class ScavengersRoundShield extends Shield {
    ScavengersRoundShield(Sprite itemLook,
                          Map<ItemAbilities, Item> itemAbilities,
                          Map<ItemBuffs, Item> itemBuffs) {
        super("ScavengersRoundShield", itemLook,
                ItemRarity.UNCOMMON, itemAbilities, itemBuffs,
                0.16, 0.38,
                ItemBuffs.BONUS_DEF, ItemBuffs.NONE, ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case BONUS_DEF -> 0.10;
            default -> 0.0;
        };
    }
}
