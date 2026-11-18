package Enchantment;

import inventory.Inventory.EquipSlot;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

// The five enchantments, the level range they share and which equipment slots take each one (enchantment spec, section 1).
// Only rules live here, no state: what one item carries is ItemEnchants, and the numbers each level is worth are
// EnchantmentConfig.
public final class Enchantment {
    private Enchantment() {}

    private static final int MAX_LEVEL = 4;
    // index is level - 1. its length hasLevel to match MAX_LEVEL
    private static final String[] ROMAN = {"I", "II", "III", "IV"};

    public enum EnchantmentType {
        SHARPNESS("Sharpness", EnumSet.of(EquipSlot.MELEE)),
        POWER("Power", EnumSet.of(EquipSlot.RANGED_WEAPON)),
        PROTECTION("Protection", EnumSet.of(EquipSlot.ARMOR)),
        // the only one a shield can take
        UNBREAKING("Unbreaking", EnumSet.allOf(EquipSlot.class)),
        LOOTING("Looting", EnumSet.of(EquipSlot.MELEE));

        private final String displayName;
        private final Set<EquipSlot> slots;

        EnchantmentType(String displayName, Set<EquipSlot> slots) {
            this.displayName = displayName;
            this.slots = Collections.unmodifiableSet(slots);
        }

        public String getDisplayName() { return displayName; }

        // the slots this enchantment can go on. cannot be modified
        public Set<EquipSlot> getSlots() { return slots; }

        public boolean appliesTo(EquipSlot equipSlot) {
            // a null would quietly read as "not allowed" otherwise
            Objects.requireNonNull(equipSlot, "equipSlot not set");
            return slots.contains(equipSlot);
        }
    }

    public static int getMaxLevel() { return MAX_LEVEL; }

    public static boolean isValidLevel(int level) { return level >= 1 && level <= MAX_LEVEL; }

    // the one place the level range is enforced
    public static void requireValidLevel(int level) {
        if (!isValidLevel(level)) {
            throw new IllegalArgumentException("Level must be between 1-" + MAX_LEVEL + ", but got: " + level);
        }
    }

    // I to IV, used in book ids
    public static String toRoman(int level) {
        requireValidLevel(level);
        return ROMAN[level - 1];
    }

    // looks an enchantment up by its exact upper case name, throws IllegalArgumentException for anything else
    public static EnchantmentType valueOf(String name) {
        return EnchantmentType.valueOf(name);
    }
}
