package Item.Armor;

import Engine.ImageLoader;
import GameObject.SpriteSheet;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class BeggarsWrapsArmor extends Armor {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.HEAL, Map.of(ItemAbilities.SECOND_WIND, 0.05),
            AbilityType.SPD_BOOST, Map.of(ItemAbilities.LIGHT_FOOTING, 0.03),
            AbilityType.DMG_REDUCTION, Map.of(ItemAbilities.HARDEN, 0.05),
            AbilityType.BONUS_DEF, Map.of(ItemAbilities.IRON_STANCE, 0.05)
    );

    public BeggarsWrapsArmor(SpriteSheet itemLook) {
        super("BeggarsWrapsArmor", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 8);
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
