package inventory;

import Item.Item;

import java.util.*;

public class Inventory {
    public enum EquipSlot { MELEE, RANGED_WEAPON, SHIELD, ARMOR }
    private static final int MAX_INVENTORY_SLOTS = 20;
    private final List<Item> items = new ArrayList<>();
    private final Map<EquipSlot, Item> equippedItems = new EnumMap<>(EquipSlot.class);

    // if the array size is less then the max num of items allowed, add it
    public void addItem(Item item) {
        if (items.size() < MAX_INVENTORY_SLOTS) items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
        equippedItems.values().remove(item); // for reference cleanup with the enum map
    }

    // removes broken items from the inventory and unequips it if needed
    public int removeBrokenItem() {
        int removedCount = 0;
        // loop through all of the items in the inventory and if the item is broken, remove it and increment the counter
        for (Item item : new ArrayList<>(items)) {
            if (item.getItemStats().isBroken()) {
                removeItem(item);
                removedCount++;
            }
        }
        return removedCount;
    }

    public void toggleItemEquip(int index) {
        if ((index < 0) || (index >= items.size())) return;
        Item item = items.get(index);
        if (isItemEquipped(item)) equippedItems.remove(item.getEquipSlot());
        else equippedItems.put(item.getEquipSlot(), item); // replace the old occupant for that slot
    }

    public boolean isItemEquipped(Item item) { return equippedItems.get(item.getEquipSlot()) == item; }
    public Item getEquippedItem(EquipSlot slot) { return equippedItems.get(slot); }

    // returns an unmodifiable map of the entire item map each time called. this doesnt get an individual index
    public List<Item> getAllEquippedItems() { return Collections.unmodifiableList(new ArrayList<>(equippedItems.values())); }

    // this gets all items, not just equipped items, again as an unmodifiable map
    public List<Item> getAllItems() { return Collections.unmodifiableList(items); }
}
