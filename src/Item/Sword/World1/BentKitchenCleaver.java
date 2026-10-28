package Item.Sword.World1;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.ItemStats;
import Item.Sword.Sword;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class BentKitchenCleaver extends Sword {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.GALE_SLASH, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.HEAVY_CLEAVE, 0.00),
            AbilityType.SUSTAIN, Map.of(ItemAbilities.BLOODFANG_STRIKE, 0.0),
            AbilityType.FINISHER, Map.of(ItemAbilities.RECKONING_BLOW, 0.00)
    );

    public BentKitchenCleaver(SpriteSheet itemLook) {
        super("BentKitchenCleaver", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 35,
                ItemStats.getDefaultsForItemRarity(ItemRarity.COMMON));
    }

    @Override
    public SpriteSheet assignSprite() {
        // load the full item sprite sheet
        BufferedImage mstrSheet = ImageLoader.load("Items.png");

        // get the subimage (the actual sprite for each item type)
        BufferedImage croppedIcon = mstrSheet.getSubimage(0, 0, 32, 32);
        return new SpriteSheet(croppedIcon, 32, 32);
    }
}
