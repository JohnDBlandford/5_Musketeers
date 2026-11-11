package Shop.Blacksmith;

import Game.PlayerData;
import Item.Item;
import Item.ItemEntry;
import Item.ItemStats;
import Item.Registry.ItemRegistry;
import inventory.Inventory;

import java.util.*;

// The blacksmith combines two copies of the same item into one at full durability (like an anvil): the better copy is kept
// and repaired, the other is used up, and the player pays BlacksmithConfig's fee in gold. Stateless: gold lives in PlayerData
// and the items live in the Inventory, so one instance can be shared.
public class Blacksmith implements IBlacksmith {
    // fee for combining copies of this item. the world comes from the registry (an Item does not store its own world), and an
    // item the registry does not know throws, so a missing fee can never become a free repair
    @Override
    public int getRepairFee(Item item) {
        Objects.requireNonNull(item, "item not found");
        ItemEntry entry = ItemRegistry.getEntry(item.getItemName());
        return BlacksmithConfig.getRepairFee(item.getItemRarity(), entry.world());
    }

    // one list per item name with 2 or more non-broken copies, in order of first appearance. a broken copy is destroyed by
    // Inventory.removeBrokenItem anyway and can't be repaired, so it is never offered
    @Override
    public List<List<Item>> getCombinableGroups(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory cannot be combined");
        Map<String, List<Item>> byName = new LinkedHashMap<>();
        for (Item item : inventory.getAllItems()) {
            if (item.getItemStats().isBroken()) continue;
            byName.computeIfAbsent(item.getItemName(), name -> new ArrayList<>()).add(item);
        }
        List<List<Item>> groups = new ArrayList<>();
        for (List<Item> copies : byName.values()) {
            if (copies.size() >= 2) groups.add(Collections.unmodifiableList(copies));
        }
        return Collections.unmodifiableList(groups);
    }

    // the copy combine() keeps: the higher level, then the one with more xp, then a (the first argument). durability does not
    // matter. checks nothing else and changes nothing, so the screen can use it to preview a combine
    @Override
    public Item pickKeeper(Item a, Item b) {
        Objects.requireNonNull(a, "a item cannot be found");
        Objects.requireNonNull(b, "b item cannot be found");
        return outranks(b, a) ? b : a;
    }

    // all checks run before anything changes, so a rejected combine costs nothing
    @Override
    public CombineResult combine(Item a, Item b, Inventory inventory) {
        Objects.requireNonNull(a, "a item not set");
        Objects.requireNonNull(b, "b item not set");
        Objects.requireNonNull(inventory, "inventory not set");

        List<Item> pack = inventory.getAllItems();
        if (!pack.contains(a) || !pack.contains(b)) return CombineResult.NOT_IN_INVENTORY;
        if (a == b) return CombineResult.SAME_COPY;
        if (!a.getItemName().equals(b.getItemName())) return CombineResult.NOT_SAME_ITEM;
        if (a.getItemStats().isBroken() || b.getItemStats().isBroken()) return CombineResult.BROKEN;

        Item keeper = pickKeeper(a, b);
        Item fodder = (keeper == a) ? b : a;

        // the fee is the last check and the only one that changes anything, and only when it succeeds
        if (!PlayerData.trySpend(getRepairFee(keeper))) return CombineResult.NOT_ENOUGH_GOLD;

        // removeItem also unequips the fodder, so the slot would be left empty. hand it to the keeper instead
        boolean fodderWasEquipped = inventory.isItemEquipped(fodder);
        inventory.removeItem(fodder);
        if (fodderWasEquipped) inventory.toggleItemEquip(inventory.getAllItems().indexOf(keeper));

        ItemStats stats = keeper.getItemStats();
        stats.repairItem(stats.getItemMaxDurability());
        return CombineResult.SUCCESS;
    }

    // true if first is strictly better than second: a higher level, or the same level and more xp
    private static boolean outranks(Item first, Item second) {
        ItemStats f = first.getItemStats();
        ItemStats s = second.getItemStats();
        if (f.getLvl() != s.getLvl()) return f.getLvl() > s.getLvl();
        return f.getXp() > s.getXp();
    }
}
