package Item.Armor.World2;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.Armor.Armor;
import Item.ItemStats;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class EnforcersChainmailArmor extends Armor {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.HEAL, Map.of(ItemAbilities.SECOND_WIND, 0.08),
            AbilityType.SPD_BOOST, Map.of(ItemAbilities.LIGHT_FOOTING, 0.05),
            AbilityType.DMG_REDUCTION, Map.of(ItemAbilities.HARDEN, 0.08),
            AbilityType.BONUS_DEF, Map.of(ItemAbilities.IRON_STANCE, 0.09)
    );

    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 0.70
    );

    public EnforcersChainmailArmor(SpriteSheet itemLook) {
        super("EnforcersChainmailArmor", itemLook, ItemRarity.UNCOMMON, ABILITIES, BUFFS, 26,
                ItemStats.getDefaultsForItemRarity(ItemRarity.UNCOMMON));
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
