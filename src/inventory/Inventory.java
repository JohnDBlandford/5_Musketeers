package inventory;

import Item.Consumable.Consumable;
import Item.Item;

import java.util.*;

public class Inventory {
    public enum EquipSlot { MELEE, RANGED_WEAPON, SHIELD, ARMOR }

    private static final int MAX_INVENTORY_SLOTS = 20;
    // consumables stack, and each stack uses one of the shared MAX_INVENTORY_SLOTS
    public static final int MAX_CONSUMABLE_STACK = 10;
    private final List<Item> items = new ArrayList<>();
    private final List<InventoryStack> consumables = new ArrayList<>();
    private final Map<EquipSlot, Item> equippedItems = new EnumMap<>(EquipSlot.class);

    // read only view of a stack of consumables

    /**
     * @author Derek
     * @param consumable the consumable in question
     * @param count the count of that consumable
     */
    public record ConsumableStack(Consumable consumable, int count) {}

    private static final class InventoryStack {
        private final Consumable consumable;
        private int count;

        /**
         * @author Derek
         * @param consumable the consumable in question
         */
        private InventoryStack(Consumable consumable) {
            this.consumable = consumable;
            this.count = 1;
        }
    }

    // gear and consumable stacks share the same slots

    /**
     * @author Derek
     * @return the combo of the item and consumable slots
     */
    public int getUsedSlots() { return items.size() + consumables.size(); }

    /**
     * @author Derek
     * @return returns true if the total used slots exceeds the max number of slots allowed
     */
    public boolean isFull() { return getUsedSlots() >= MAX_INVENTORY_SLOTS; }

    // if the array size is less then the max num of items allowed, add it
    // returns false when the inventory is full so shops and chests can refuse the transaction instead of losing the item

    /**
     * @author Derek
     * @param item the item to add
     * @return returns true and adds the item to the inventory if it isnt full yet
     */
    public boolean addItem(Item item) {
        if (isFull()) return false;
        return items.add(item);
    }

    // true if count more gear items would all fit. a shop checks this before taking gold

    /**
     * @author Derek
     * @param count the number of items to add
     * @return adds the items to the inventory if they fit
     */
    public boolean canAddItems(int count) {
        if (count < 0) throw new IllegalArgumentException("Item count cannot be negative: " + count);
        return getUsedSlots() + count <= MAX_INVENTORY_SLOTS;
    }

    // true if qty of this consumable would ALL fit, so a shop never takes gold for a purchase that only partly fits.
    // room in partial stacks of the same consumable is used first, then whatever is left needs new stacks of
    // MAX_CONSUMABLE_STACK, and those new stacks must fit in the free slots. this does not change the inventory

    /**
     * @author Derek
     * @param consumable the consumable to add
     * @param qty the quantity of the consumable to add
     * @return returns true if the consumable with said quantity if there is space
     */
    public boolean canAddConsumables(Consumable consumable, int qty) {
        Objects.requireNonNull(consumable, "consumable cannot be added");
        if (qty < 0) throw new IllegalArgumentException("Consumable quantity cannot be negative: " + qty);

        int remaining = qty;
        for (InventoryStack inventoryStack : consumables) {
            // if the stacks consumable matches the method consumable, subtract qty from the MAX_CONSUMABLE_STACK minus
            // the total inventoryStack count
            if (inventoryStack.consumable.getItemName().equals(consumable.getItemName())) {
                remaining -= MAX_CONSUMABLE_STACK - inventoryStack.count;
            }
        }
        if (remaining <= 0) return true;

        int newStacksNeeded = (remaining + MAX_CONSUMABLE_STACK - 1) / MAX_CONSUMABLE_STACK; // rounds up
        return getUsedSlots() + newStacksNeeded <= MAX_INVENTORY_SLOTS;
    }

    // adds one to a stack of that consumable with room, otherwise starts a new stack. false if there is no room
    public boolean addConsumable(Consumable consumable) {
        Objects.requireNonNull(consumable, "consumable cannot be null");
        InventoryStack inventoryStack = findStackWithRoom(consumable.getItemName());
        if (inventoryStack != null) {
            inventoryStack.count++;
            return true;
        }
        if (isFull()) return false;
        consumables.add(new InventoryStack(consumable));
        return true;
    }

    // uses up one of the named consumable and returns it so the caller can apply its effect, or null if there are none
    // the smallest stack is used first so partial stacks clear out and free their slot

    /**
     * @author Derek
     * @param itemName the name of the consumable to add
     * @return returns the smallest stack
     */
    public Consumable useConsumable(String itemName) {
        InventoryStack smallest = null;
        for (InventoryStack inventoryStack : consumables) {
            if (inventoryStack.consumable.getItemName().equals(itemName) && (smallest == null || inventoryStack.count < smallest.count)) smallest = inventoryStack;
        }
        if (smallest == null) return null;
        smallest.count--;
        if (smallest.count == 0) consumables.remove(smallest);
        return smallest.consumable;
    }

    /**
     * @author Derek
     * @param itemName the name of the item with the count
     * @return the total consumable count of the item by its name
     */
    public int getConsumableCount(String itemName) {
        int total = 0;
        for (InventoryStack inventoryStack : consumables) if (inventoryStack.consumable.getItemName().equals(itemName)) total += inventoryStack.count;
        return total;
    }

    // a snapshot of every stack, in the order they were started

    /**
     * @author Derek
     * @return returns an unmodifiable list of all consumables
     */
    public List<ConsumableStack> getAllConsumables() {
        List<ConsumableStack> view = new ArrayList<>();
        for (InventoryStack inventoryStack : consumables) view.add(new ConsumableStack(inventoryStack.consumable, inventoryStack.count));
        return Collections.unmodifiableList(view);
    }

    /**
     * @author Derek
     * @param itemName the items name to find room
     * @return returns the next free full stack
     */
    private InventoryStack findStackWithRoom(String itemName) {
        for (InventoryStack inventoryStack : consumables) {
            if (inventoryStack.consumable.getItemName().equals(itemName) && inventoryStack.count < MAX_CONSUMABLE_STACK) return inventoryStack;
        }
        return null;
    }

    /**
     * @author Derek
     * @param item the item to remove
     */
    public void removeItem(Item item) {
        items.remove(item);
        equippedItems.values().remove(item); // for reference cleanup with the enum map
    }

    // removes broken items from the inventory and unequips it if needed
    // this is intentional: an item that hits 0 durability is destroyed. the blacksmith only repairs items that still have
    // durability left, so a broken item can never reach it

    /**
     * @author Derek & John
     * @return returns the count of how many items have been removed
     */
    public int removeBrokenItem() {
        int removedCount = 0;
        // loop through all of the items in the inventory and if the item is broken,
        // remove it and increment the counter
        for (Item item : new ArrayList<>(items)) {
            if (item.getItemStats().isBroken()) {
                removeItem(item);
                removedCount++;
            }
        }
        return removedCount;
    }

    /**
     * @author Derek
     * @param index the index where the item is
     */
    public void toggleItemEquip(int index) {
        if ((index < 0) || (index >= items.size())) return;
        Item item = items.get(index);
        if (isItemEquipped(item)) equippedItems.remove(item.getEquipSlot());
        else equippedItems.put(item.getEquipSlot(), item); // replace the old occupant for that slot
    }

    // removes all items from the inventory and unequips them. Needed for save
    // loading
    public void clear() {
        items.clear();
        equippedItems.clear();
    }

    public boolean isItemEquipped(Item item) {
        Objects.requireNonNull(item, "item not found");
        return equippedItems.get(item.getEquipSlot()) == item;
    }

    public Item getEquippedItem(EquipSlot slot) {
        return equippedItems.get(slot);
    }

    // returns an unmodifiable map of the entire item map each time called. this
    // doesnt get an individual index
    public List<Item> getAllEquippedItems() {
        return Collections.unmodifiableList(new ArrayList<>(equippedItems.values()));
    }

    // this gets all items, not just equipped items, again as an unmodifiable map
    // consumables are not in this list, use getAllConsumables()
    public List<Item> getAllItems() { return Collections.unmodifiableList(items); }
}
