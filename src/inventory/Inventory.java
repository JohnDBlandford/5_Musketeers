package inventory;

import Item.Item;

import java.util.*;

public class Inventory {
    private static final int MAX_INVENTORY_SLOTS = 20;
    private final List<Item> backpack = new ArrayList<>();
    private final Map<EquipSlot, Item> equippedItems = new EnumMap<>(EquipSlot.class);

    public boolean equipItem(int index) {
        if (index < 0 || index >= backpack.size()) return false;
        Item item = backpack.get(index);
        Item prev = equippedItems.put(item.getEquipSlot(), item);
        // swap in place to prevent item shifts when items are removed
        if (prev != null) backpack.set(index, prev);
        else backpack.remove(index); // empty slot, list scales dynamically
        return true;
    }

    public boolean unequip(EquipSlot equipSlot) {
        // if the enum map does not contain the same key and the backpack is full, return false
        if (!equippedItems.containsKey(equipSlot) || backpack.size() >= MAX_INVENTORY_SLOTS) return false;
        backpack.add(equippedItems.remove(equipSlot));
        return true;
    }

    // nulls are allowed here because they represent empty/unused slots
    public Item getEquipped(EquipSlot equipSlot) { return equippedItems.get(equipSlot); }

    // returns an unmodifiable map of the backpack at the current time its called, if the map gets updated, this must be called again
    public List<Item> getBackpack() { return Collections.unmodifiableList(backpack); }
}
