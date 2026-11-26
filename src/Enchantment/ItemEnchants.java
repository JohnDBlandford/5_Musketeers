package Enchantment;

import Enchantment.Enchantment.EnchantmentType;
import inventory.Inventory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

// The enchantments one item carries and their levels (enchantment spec, sections 1, 4 and 5). Every Item owns exactly one,
// the way it owns its own ItemStats, so two copies of the same sword never share enchantments. Plain data: it never touches
// the item's buff map, and the battle code reads it through Item.getEnchants().
public final class ItemEnchants {
    // the first four come from apply. the last two are for the enchant table, which also checks the pack and the books
    public enum ApplyResult {
        APPLIED, REPLACED, WRONG_ITEM_TYPE, NOT_HIGHER, NOT_IN_INVENTORY, NO_BOOK
    }

    // an EnumMap keeps every listing in the enchantment order, whatever order they were applied in
    private final EnumMap<EnchantmentType, Integer> levels = new EnumMap<>(EnchantmentType.class);

    public ItemEnchants() {}

    // the level held, 0 if none
    public int getLevel(EnchantmentType enchantment) {
        Objects.requireNonNull(enchantment, "enchantment not set");
        return levels.getOrDefault(enchantment, 0);
    }

    public boolean hasLevel(EnchantmentType enchantment) { return getLevel(enchantment) > 0; }

    public boolean isEmpty() { return levels.isEmpty(); }

    public int size() { return levels.size(); }

    // a copy of what is held right now, so a later apply never changes a map someone is already holding
    public Map<EnchantmentType, Integer> asMap() {
        return Collections.unmodifiableMap(new EnumMap<>(levels));
    }

    // puts an enchantment on the item. the checks run in this order and nothing changes unless it returns APPLIED or REPLACED:
    //   null enchantment or slot       NullPointerException
    //   level outside 1 to max level   IllegalArgumentException (checked before the slot)
    //   wrong slot                     WRONG_ITEM_TYPE
    //   same or lower level held       NOT_HIGHER (the caller keeps the book)
    //   a lower level held             REPLACED
    //   none held                      APPLIED
    public ApplyResult apply(EnchantmentType enchantment, int level, Inventory.EquipSlot equipSlot) {
        Objects.requireNonNull(enchantment, "enchantment not set");
        Objects.requireNonNull(equipSlot, "equipSlot not set");
        Enchantment.requireValidLevel(level);
        if (!enchantment.appliesTo(equipSlot)) return ApplyResult.WRONG_ITEM_TYPE;

        int held = getLevel(enchantment);
        if (level <= held) return ApplyResult.NOT_HIGHER;
        levels.put(enchantment, level);
        return held == 0 ? ApplyResult.APPLIED : ApplyResult.REPLACED;
    }

    // the Blacksmith's transfer rule: for each enchantment keep the higher of the two levels, so enchants always survive a
    // combine. equal levels stay as they are and are never added together, which would get around the world cap.
    // other is not changed and slots are not checked (the Blacksmith only ever merges two copies of the same item)
    public void mergeHigher(ItemEnchants other) {
        Objects.requireNonNull(other, "other enchants not set");
        if (other == this) return;
        for (Map.Entry<EnchantmentType, Integer> entry : other.levels.entrySet()) {
            levels.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    // "" when empty, otherwise NAME:level pairs in enchantment order joined by commas, e.g. "SHARPNESS:2,UNBREAKING:1"
    public String toSaveString() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<EnchantmentType, Integer> entry : levels.entrySet()) {
            if (!text.isEmpty()) text.append(',');
            text.append(entry.getKey().name()).append(':').append(entry.getValue());
        }
        return text.toString();
    }

    // the reverse of toSaveString. null or blank (a save with no enchants key) gives an empty ItemEnchants. anything malformed
    // throws IllegalArgumentException: no colon, no or a non-numeric level, a level outside 1 to max level, an unknown or lower
    // case name, an empty entry, or the same name twice. It does not know which item the text belongs to, so it does not
    // check slots
    public static ItemEnchants parse(String saveString) {
        ItemEnchants parsed = new ItemEnchants();
        if (saveString == null || saveString.isBlank()) return parsed;

        // -1 keeps empty entries, so "A:1,,B:1" and a trailing comma are caught instead of skipped
        for (String entry : saveString.split(",", -1)) {
            final int colon = entry.indexOf(':');
            if (colon <= 0 || colon == entry.length() - 1) {
                throw new IllegalArgumentException("Enchantment entry must look like NAME:level, but got: '" + entry + "'");
            }
            EnchantmentType type = Enchantment.valueOf(entry.substring(0, colon));
            int level;
            try {
                level = Integer.parseInt(entry.substring(colon + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Enchantment level is not a number in: '" + entry + "'", e);
            }
            Enchantment.requireValidLevel(level);
            if (parsed.levels.containsKey(type)) {
                throw new IllegalArgumentException("Enchantment listed twice: " + type);
            }
            parsed.levels.put(type, level);
        }
        return parsed;
    }
}
