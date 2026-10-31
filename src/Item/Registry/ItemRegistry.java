package Item.Registry;

import Item.Item;
import Item.ItemEntry;
import Item.Stockable;
import Item.Consumable.Consumable;

import java.util.*;
import java.util.function.Supplier;

// Single place that knows every item that can exist in the game, keyed by String id.
// The merchant and the chests only ever call query...() and create...(). Weighting by rarity, the item shop size and the 30 min
// reroll belong to those systems and not here.
// To add items for a new world: make a WorldNItems class with a registerAll() and add one line to the static block below.
public final class ItemRegistry {
    // LinkedHashMap so query results come back in a stable registration order
    private static final Map<String, ItemEntry> ENTRIES = new LinkedHashMap<>();

    static {
        World1Items.registerAll();
        World2Items.registerAll();
        World3Items.registerAll();
    }

    private ItemRegistry() {}

    // registers a piece of gear
    public static void register(String id, int world, Item.ItemRarity rarity, Supplier<Item> factory) {
        add(new ItemEntry(id, world, rarity, ItemEntry.ItemKind.GEAR, factory::get));
    }

    // registers a consumable item
    public static void registerConsumable(String id, int world, Item.ItemRarity rarity, Supplier<Consumable> factory) {
        add(new ItemEntry(id, world, rarity, ItemEntry.ItemKind.CONSUMABLE, factory::get));
    }

    // adds an entry into the map
    private static void add(ItemEntry entry) {
        if (ENTRIES.putIfAbsent(entry.id(), entry) != null) throw new IllegalStateException("Duplicate item id: " + entry.id());
    }

    public static boolean containsId(String id) { return ENTRIES.containsKey(id); }

    // gets an entry from the map via its id
    public static ItemEntry getEntry(String id) {
        ItemEntry entry = ENTRIES.get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown item id: " + id);
        return entry;
    }

    // builds a fresh Item or Consumable
    public static Stockable create(String id) { return getEntry(id).create(); }

    // typed versions of create(), these throw if the id is the other kind
    public static Item createItem(String id) {
        if (create(id) instanceof Item item) return item;
        throw new IllegalArgumentException(id + " is not gear");
    }

    public static Consumable createConsumable(String id) {
        if (create(id) instanceof Consumable consumable) return consumable;
        throw new IllegalArgumentException(id + " is not a consumable");
    }

    // GEAR entries whose world is within [minWorld, maxWorld] and have the given rarity. consumables are never returned here
    // so the rarity curve only ever applies to gear
    public static List<ItemEntry> query(int minWorld, int maxWorld, Item.ItemRarity rarity) {
        List<ItemEntry> result = new ArrayList<>();
        for (ItemEntry entry : ENTRIES.values()) {
            // if the entry is gear and the world is between the min and max worlds for that piece of gear, add it to the list
            if (entry.itemKind() == ItemEntry.ItemKind.GEAR && entry.world() >= minWorld && entry.world() <= maxWorld
                    && entry.rarity() == rarity) result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    // world 1 up through maxWorld, which is the "world 2 shows 1 and 2" rule. world 4 passes LEGENDARY and gets the
    // legendaries of every world up to 4. gear only
    public static List<ItemEntry> queryUpToWorld(int maxWorld, Item.ItemRarity rarity) {
        return query(ItemEntry.getMinWorld(), maxWorld, rarity);
    }

    // every consumable available up through maxWorld at any rarity. the merchant uses this for its guaranteed food and
    // water slots, which sit outside the rarity curve
    public static List<ItemEntry> queryConsumablesUpToWorld(int maxWorld) {
        List<ItemEntry> result = new ArrayList<>();
        for (ItemEntry entry : ENTRIES.values()) {
            if (entry.itemKind() == ItemEntry.ItemKind.CONSUMABLE && entry.world() <= maxWorld) result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<ItemEntry> getAllEntries() { return List.copyOf(ENTRIES.values()); }

    // builds every entry once and checks the entry agrees with what it builds. call this from a test or once at startup
    // while developing. returns the problems found, an empty list means the registry is consistent
    public static List<String> validateEntry() {
        List<String> problems = new ArrayList<>();
        for (ItemEntry entry : ENTRIES.values()) {
            Stockable built;
            try {
                built = entry.create();
            } catch (RuntimeException e) {
                problems.add(entry.id() + ": factory threw " + e);
                continue;
            }
            if (!entry.id().equals(built.getItemName())) problems.add(entry.id() + ": name is " + built.getItemName());
            if (entry.rarity() != built.getItemRarity()) problems.add(entry.id() + ": entry says " + entry.rarity() + " but it is " + built.getItemRarity());
            boolean builtIsGear = built instanceof Item;
            if (builtIsGear != (entry.itemKind() == ItemEntry.ItemKind.GEAR)) problems.add(entry.id() + ": entry kind is " + entry.itemKind() + " but it builds " + built.getClass().getSimpleName());
        }
        return problems;
    }
}
