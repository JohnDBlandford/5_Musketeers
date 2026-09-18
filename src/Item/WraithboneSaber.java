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

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case CRIT_CHANCE -> 0.10;
            case ATK_BOOST -> 0.14;
            case CRIT_DMG -> 0.25;
            default -> 0.0;
        };
    }
}
