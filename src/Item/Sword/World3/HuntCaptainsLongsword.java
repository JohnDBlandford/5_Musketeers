package Item.Sword.World3;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.ItemStats;
import Item.Sword.Sword;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class HuntCaptainsLongsword extends Sword {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.GALE_SLASH, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.HEAVY_CLEAVE, 0.00),
            AbilityType.SUSTAIN, Map.of(ItemAbilities.BLOODFANG_STRIKE, 0.0),
            AbilityType.FINISHER, Map.of(ItemAbilities.RECKONING_BLOW, 0.00)
    );

    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.13,
            ItemBuffs.ATK_BOOST, 0.16
    );

    public HuntCaptainsLongsword(SpriteSheet itemLook) {
        super("HuntCaptainsLongsword", itemLook, ItemRarity.EPIC, ABILITIES, BUFFS, 190,
                ItemStats.getDefaultsForItemRarity(ItemRarity.EPIC));
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
