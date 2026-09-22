package Item;

import GameObject.Sprite;

import java.util.Map;

public final class ScavengersRoundShield extends Shield {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.10
    );
    ScavengersRoundShield(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities) {
        super("ScavengersRoundShield", itemLook, ItemRarity.UNCOMMON, itemAbilities, BUFFS, 0.16, 0.38);
    }
}
