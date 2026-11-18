package Wear;

import Enchantment.EnchantEffects;
import Item.Item;
import inventory.Inventory;

import java.util.Objects;
import java.util.Random;

// One per battle. Lowers the durability of the knight's gear and rolls Unbreaking for every point.
// It makes no decisions about the battle, only about durability: PlayBattleScreen just calls it. The random is used ONLY for
// Unbreaking rolls, never the battle's own rng.
public class GearWear {
    private final Inventory inventory;
    private final Random random;
    // false in test mode, when the knight cannot be hurt
    private boolean enabled = true;

    public GearWear(Inventory inv, Random random) {
        this.inventory = Objects.requireNonNull(inv, "inventory not set");
        this.random = Objects.requireNonNull(random, "random not set");
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) { this.enabled = value; }

    // lowers the item's durability by points, rolling Unbreaking for each point on its own (an ignored point costs nothing).
    // returns the durability actually lost: never more than the item had left, 0 for a null or broken item or while disabled.
    // negative points throw
    public int wear(Item item, int points) {
        if (points < 0) throw new IllegalArgumentException("Points cannot be negative, but got: " + points);
        if (!enabled || item == null) return 0;
        int applied = 0;
        for (int i = 0; i < points; i++) { if (!EnchantEffects.ignoresDurabilityHit(item, random)) applied++; }
        int before = item.getItemStats().getDurability();
        item.getItemStats().damageItem(applied);
        return before - item.getItemStats().getDurability();
    }

    // the knight used a damaging ability: wears the equipped item in that slot. a shield move wears the shield. an empty slot
    // (bare hands) is fine and returns 0. armor is not a weapon slot
    public int wearWeapon(Inventory.EquipSlot equipSlot) {
        Objects.requireNonNull(equipSlot, "equip slot not set");
        if (equipSlot == Inventory.EquipSlot.ARMOR) throw new IllegalArgumentException("Armor is not a weapon slot");
        return wear(inventory.getEquippedItem(equipSlot), WearConfig.getWeaponPtsPerAtk());
    }

    // an enemy attack landed
    public int wearArmor() { return wear(inventory.getEquippedItem(Inventory.EquipSlot.ARMOR), WearConfig.getArmorPtsPerHit()); }

    // the shield took a blow
    public int wearShield() { return wear(inventory.getEquippedItem(Inventory.EquipSlot.SHIELD), WearConfig.getShieldPtsPerBlck()); }
}
