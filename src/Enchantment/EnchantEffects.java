package Enchantment;

import Enchantment.Enchantment.EnchantmentType;
import Item.Item;
import inventory.Inventory;

import java.util.Objects;
import java.util.Random;

// Battle lookups. Only READS enchants and turns them into numbers; it never changes an item,
// an inventory or any other state. The 75% damage reduction cap stays in BattleMath.
public class EnchantEffects {
    // does one point of durability damage get ignored? true when random.nextDouble() is strictly below the Unbreaking chance.
    // an item without Unbreaking never ignores and does not roll
    public static boolean ignoresDurabilityHit(Item item, Random random) {
        Objects.requireNonNull(item, "item not set");
        Objects.requireNonNull(random, "random not set");
        int level = item.getEnchants().getLevel(EnchantmentType.UNBREAKING);
        if (level == 0) return false;
        return random.nextDouble() < EnchantmentConfig.getEffect(EnchantmentType.UNBREAKING, level);
    }

    // the ATK fraction a weapon's own enchants add: Sharpness on a melee weapon, Power on a ranged weapon. null (bare hands) and
    // any other kind of item give 0.0
    public static double attackBonus(Item item) {
        if (item == null) return 0.0;
        return switch (item.getEquipSlot()) {
            case MELEE -> EnchantmentConfig.getEffect(EnchantmentType.SHARPNESS, item.getEnchants()
                    .getLevel(EnchantmentType.SHARPNESS));
            case RANGED_WEAPON -> EnchantmentConfig.getEffect(EnchantmentType.POWER, item.getEnchants()
                    .getLevel(EnchantmentType.POWER));
            default -> 0.0;
        };
    }

    // the damage reduction fraction Protection adds, over the EQUIPPED items only
    public static double damageReductionBonus(Inventory inv) {
        Objects.requireNonNull(inv, "inventory not set");
        double total = 0.0;
        for (Inventory.EquipSlot slot : Inventory.EquipSlot.values()) {
            Item item = inv.getEquippedItem(slot);
            if (item != null) total += EnchantmentConfig.getEffect(EnchantmentType.PROTECTION, item.getEnchants()
                    .getLevel(EnchantmentType.PROTECTION));
        }
        return total;
    }

    // the Looting level on the equipped melee weapon, 0 if there is none
    public static int lootingLevel(Inventory inv) {
        Objects.requireNonNull(inv, "inventory not set");
        Item weapon = inv.getEquippedItem(Inventory.EquipSlot.MELEE);
        return weapon == null ? 0 : weapon.getEnchants().getLevel(EnchantmentType.LOOTING);
    }

    // the EXTRA gold Looting adds to base (halves round up). the caller adds it to base. negative base throws
    public static int goldBonus(Inventory inv, int base) {
        Objects.requireNonNull(inv, "inventory not set");
        if (base < 0) throw new IllegalArgumentException("Base gold cannot be negative, but got: " + base);
        return (int) Math.round(base * EnchantmentConfig.getEffect(EnchantmentType.LOOTING, lootingLevel(inv)));
    }
}
