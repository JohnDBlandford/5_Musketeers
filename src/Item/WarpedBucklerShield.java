package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WarpedBucklerShield extends Shield {
    WarpedBucklerShield(Sprite itemLook,
                        Map<ItemAbilities, Item> itemAbilities) {
        super("WarpedBucklerShield", itemLook,
                ItemRarity.COMMON, itemAbilities, Map.of(),
                0.10, 0.28);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }
}
