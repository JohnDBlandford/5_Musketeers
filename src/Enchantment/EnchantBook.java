package Enchantment;

import Enchantment.Enchantment.EnchantmentType;
import Item.Item;
import Item.Stockable;

import java.util.Objects;

// One enchantment book (enchantment spec, section 2): an enchantment at a level, 5 x 4 = 20 of them. Books are stateless and
// immutable, so one shared instance per book is safe, and two books with the same enchantment and level are equal. A book is a
// Stockable but not an Item: it has no stats, durability or equip slot. The registry generates all 20 instead of 20 classes.
public final class EnchantBook implements Stockable {
    // the level that gets each rarity, index is level - 1 (PLACEHOLDER: the registry needs a rarity for every entry, and books
    // never go through the gear rarity curve, so this only labels them). its length has to match Enchantment.getMaxLevel()
    private static final Item.ItemRarity[] RARITY_BY_LEVEL = {
            Item.ItemRarity.COMMON, Item.ItemRarity.UNCOMMON, Item.ItemRarity.EPIC, Item.ItemRarity.LEGENDARY
    };

    private final EnchantmentType enchantment;
    private final int level;

    // throws NullPointerException for a null enchantment and IllegalArgumentException for a level outside 1 to the max level
    public EnchantBook(EnchantmentType enchantment, int level) {
        Objects.requireNonNull(enchantment, "enchantment not set");
        Enchantment.requireValidLevel(level);
        this.enchantment = enchantment;
        this.level = level;
    }

    // the registry id, the inventory stack key and the save key: the display name, "Book", then the Roman numeral,
    // e.g. "SharpnessBookII". ItemNames.getPrettyName turns it into "Sharpness Book II"
    public static String idFor(EnchantmentType enchantment, int level) {
        Objects.requireNonNull(enchantment, "enchantment not set");
        return enchantment.getDisplayName() + "Book" + Enchantment.toRoman(level);
    }

    public EnchantmentType getEnchantment() { return enchantment; }

    public int getLevel() { return level; }

    // "Sharpness II", the text the inventory info line shows for an enchantment
    public String getDisplayName() { return enchantment.getDisplayName() + " " + Enchantment.toRoman(level); }

    @Override
    public String getItemName() { return idFor(enchantment, level); }

    @Override
    public Item.ItemRarity getItemRarity() { return RARITY_BY_LEVEL[level - 1]; }

    @Override
    public boolean equals(Object other) {
        return other instanceof EnchantBook book && book.enchantment == enchantment && book.level == level;
    }

    @Override
    public int hashCode() { return Objects.hash(enchantment, level); }
}
