package Enchantment;

import Enchantment.Enchantment.EnchantmentType;

import java.util.Objects;

// Every tunable number the enchantments use, in one place, so balancing never means hunting through logic.
// ALL of these are PLACEHOLDERS (see the enchantment spec, sections 1 and 2). If one changes, update the matching test in
// Tests/EnchantConfigTests and add a line to the spec's changelog.
public final class EnchantmentConfig {
    private EnchantmentConfig() {}

    // what one level is worth, as a fraction: +3% ATK, +3% ATK, 4% damage reduction, a 15% chance that a point of
    // durability damage is ignored, +10% gold
    private static final double SHARPNESS_PER_LEVEL = 0.03;
    private static final double POWER_PER_LEVEL = 0.03;
    private static final double PROTECTION_PER_LEVEL = 0.04;
    private static final double UNBREAKING_PER_LEVEL = 0.15;
    private static final double LOOTING_PER_LEVEL = 0.10;

    // about 5 enemy kills of the book's own world. index 0 is level I, and a book's level is also the world it is sold in.
    // level IV (World 4, Final) is a guess, no Final world enemies exist yet to calibrate against
    private static final int[] BOOK_PRICES = {110, 375, 600, 1200};

    // merchant: how many of its slots hold books, and how many books each one stocks
    private static final int BOOK_SLOTS = 2;
    private static final int BOOK_STOCK_PER_SLOT = 3;
    // chests: the share of a chest's items that are books (the consumable share is ChestConfig's)
    private static final double BOOK_SHARE = 0.20;
    // inventory: books of one kind per stack
    private static final int MAX_BOOK_STACK = 10;

    public static int getBookSlots() { return BOOK_SLOTS; }
    public static int getBookStockPerSlot() { return BOOK_STOCK_PER_SLOT; }
    public static double getBookShare() { return BOOK_SHARE; }
    public static int getMaxBookStack() { return MAX_BOOK_STACK; }

    // gold for one book of this level (1 to the max level), throws IllegalArgumentException for any other level
    public static int getBookPrice(int level) {
        Enchantment.requireValidLevel(level);
        return BOOK_PRICES[level - 1];
    }

    // the value of one level of this enchantment
    public static double getEffectPerLevel(EnchantmentType type) {
        Objects.requireNonNull(type, "enchantment type not set");
        return switch (type) {
            case SHARPNESS -> SHARPNESS_PER_LEVEL;
            case POWER -> POWER_PER_LEVEL;
            case PROTECTION -> PROTECTION_PER_LEVEL;
            case UNBREAKING -> UNBREAKING_PER_LEVEL;
            case LOOTING -> LOOTING_PER_LEVEL;
        };
    }

    // the value at a level: per level value * level. level 0 means not enchanted and is worth 0.0, anything below 0 or
    // above the max level throws IllegalArgumentException
    public static double getEffect(EnchantmentType type, int level) {
        Objects.requireNonNull(type, "enchantment type not set");
        if (level < 0 || level > Enchantment.getMaxLevel()) {
            throw new IllegalArgumentException("Level must be between 0-" + Enchantment.getMaxLevel() + ", but got: " + level);
        }
        return getEffectPerLevel(type) * level;
    }
}
