package Item.Armor.World3;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.Armor.Armor;
import Item.ItemStats;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class HuntCaptainsWarplate extends Armor {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.HEAL, Map.of(ItemAbilities.SECOND_WIND, 0.12),
            AbilityType.SPD_BOOST, Map.of(ItemAbilities.LIGHT_FOOTING, 0.08),
            AbilityType.DMG_REDUCTION, Map.of(ItemAbilities.HARDEN, 0.12),
            AbilityType.BONUS_DEF, Map.of(ItemAbilities.IRON_STANCE, 0.15)
    );

    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 1.80,
            ItemBuffs.SPD_BOOST, 0.12
    );

    public HuntCaptainsWarplate(SpriteSheet itemLook) {
        super("HuntCaptainsWarplate", itemLook, ItemRarity.EPIC, ABILITIES, BUFFS, 80,
                ItemStats.getDefaultsForItemRarity(ItemRarity.EPIC));
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
