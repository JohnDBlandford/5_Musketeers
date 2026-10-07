package inventory;

import Item.Item;

import java.util.*;

public class Inventory {
    private static final int MAX_INVENTORY_SLOTS = 20;
    private final List<Item> backpack = new ArrayList<>();
    private final Map<EquipSlot, Item> equippedItems = new EnumMap<>(EquipSlot.class);

    public boolean equipItem(Item item) {
        if (!backpack.remove(item)) return false; // if the backpack cannot remove the item, return false
        // we need to track the previous item added to the map
        Item prev = equippedItems.put(item.getEquipSlot(), item); // returns the old occupant or null
        if (prev != null) backpack.add(prev);
        return true; // item can be added
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
