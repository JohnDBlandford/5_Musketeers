package Item.Armor.World3;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.Armor.Armor;
import Item.ItemStats;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class ShepherdsVigilRaimentArmor extends Armor {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.HEAL, Map.of(ItemAbilities.SECOND_WIND, 0.18),
            AbilityType.SPD_BOOST, Map.of(ItemAbilities.LIGHT_FOOTING, 0.12),
            AbilityType.DMG_REDUCTION, Map.of(ItemAbilities.HARDEN, 0.16),
            AbilityType.BONUS_DEF, Map.of(ItemAbilities.IRON_STANCE, 0.22),
            AbilityType.HEALTH_RESTORE, Map.of(ItemAbilities.GRIT, 0.15)
    );

    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 2.20,
            ItemBuffs.SPD_BOOST, 0.15,
            ItemBuffs.DMG_REDUCTION, 0.10
    );

    public ShepherdsVigilRaimentArmor(SpriteSheet itemLooks) {
        super("ShepherdsVigilRaimentArmor", itemLooks, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 100,
                ItemStats.getDefaultsForItemRarity(ItemRarity.LEGENDARY));
    }

    @Override
    public SpriteSheet assignSprite() {
        // load the full item sprite sheet
        BufferedImage mstrSheet = ImageLoader.load("Items.png");

        int row = 1;
        int col = 1;

        // get the subimage (the actual sprite for each item type)
        BufferedImage croppedIcon = mstrSheet.getSubimage(col * 32, row * 32, 32, 32);
        return new SpriteSheet(croppedIcon, 32, 32);
    }
}
