package Item.Registry;

import Enchantment.EnchantBook;
import Enchantment.Enchantment;
import GameObject.SpriteSheet;
import Item.Item;
import Item.ItemEntry;
import Item.Stockable;
import Item.Consumable.Consumable;

import java.util.*;
import java.util.function.Function;

// Single place that knows every item that can exist in the game, keyed by String id.
// The merchant and the chests only ever call query...() and create...(). Weighting by rarity, the item shop size and the 30 min
// reroll belong to those systems and not here.
// To add items for a new world: make a WorldNItems class with a registerAll() and add one line to the static block below.
public final class ItemRegistry {
    // LinkedHashMap so query results come back in a stable registration order
    private static final Map<String, ItemEntry> ENTRIES = new LinkedHashMap<>();

    // registers all items with this class
    static {
        World1Items.registerAll();
        World2Items.registerAll();
        World3Items.registerAll();
        World4Items.registerAll();
        registerAllBooks();
    }

    private ItemRegistry() {}

    // registers a piece of gear. the id and rarity are read from the item itself, so they can never disagree with it.
    // one prototype is built here just to read them. sprites are passed as null because Item.getItemLook() loads lazily
    public static void register(int world, final Function<SpriteSheet, Item> constructor) {
        Item prototype = constructor.apply(null);
        addEntry(new ItemEntry(prototype.getItemName(), world, prototype.getItemRarity(), ItemEntry.ItemKind.GEAR,
                () -> constructor.apply(null)));
    }

    // registers a consumable. they are immutable so every create() hands back the same shared instance
    public static void registerConsumable(final int world, Consumable consumable) {
        addEntry(new ItemEntry(consumable.getItemName(), world, consumable.getItemRarity(), ItemEntry.ItemKind.CONSUMABLE,
                () -> consumable));
    }

    // registers an enchantment book. like consumables they are immutable, so every create() hands back the same shared instance.
    // its world is its level, so a book is sold and found in the world that matches it
    public static void registerBook(final EnchantBook book) {
        addEntry(new ItemEntry(book.getItemName(), book.getLevel(), book.getItemRarity(), ItemEntry.ItemKind.BOOK, () -> book));
    }

    // all 20 books, one per enchantment and level, generated here instead of in a WorldNItems class
    private static void registerAllBooks() {
        for (Enchantment.EnchantmentType type : Enchantment.EnchantmentType.values()) {
            for (int level = 1; level <= Enchantment.getMaxLevel(); level++) registerBook(new EnchantBook(type, level));
        }
    }

    // adds an entry into the map
    private static void addEntry(final ItemEntry entry) {
        if (ENTRIES.putIfAbsent(entry.id(), entry) != null) throw new IllegalStateException("Duplicate item id: " + entry.id());
    }

    // checks if the map contains the specified key
    public static boolean containsId(final String id) { return ENTRIES.containsKey(id); }

    // gets an entry from the map via its id
    public static ItemEntry getEntry(final String id) {
        ItemEntry entry = ENTRIES.get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown item id: " + id);
        return entry;
    }

    // builds a fresh Item, or the shared Consumable or EnchantBook
    public static Stockable create(final String id) { return getEntry(id).create(); }

    // typed versions of create(), these throw if the id is the other kind
    public static Item createItem(final String id) {
        if (create(id) instanceof Item item) return item;
        throw new IllegalArgumentException(id + " is not gear");
    }

    public static Consumable createConsumable(final String id) {
        if (create(id) instanceof Consumable consumable) return consumable;
        throw new IllegalArgumentException(id + " is not a consumable");
    }

    // like createItem and createConsumable, this throws if the id is another kind
    public static EnchantBook createBook(final String id) {
        if (create(id) instanceof EnchantBook book) return book;
        throw new IllegalArgumentException(id + " is not a book");
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
    public static List<ItemEntry> queryUpToWorld(final int maxWorld, Item.ItemRarity rarity) {
        return query(ItemEntry.getMinWorld(), maxWorld, rarity);
    }

    // every consumable available up through maxWorld at any rarity. the merchant uses this for its guaranteed food and
    // water slots, which sit outside the rarity curve
    public static List<ItemEntry> queryConsumablesUpToWorld(final int maxWorld) {
        List<ItemEntry> result = new ArrayList<>();
        for (ItemEntry entry : ENTRIES.values()) {
            if (entry.itemKind() == ItemEntry.ItemKind.CONSUMABLE && entry.world() <= maxWorld) result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    // the books of exactly this world, one per enchantment in enchantment order, all at the level that matches the world. "Exactly",
    // not "up to": the merchant sells a book at the level of the world it is in, so a World 2 shop never offers a level I book
    public static List<ItemEntry> queryBooksForWorld(final int world) {
        ItemEntry.requireValidWorld(world);
        List<ItemEntry> result = new ArrayList<>();
        for (ItemEntry entry : ENTRIES.values()) {
            if (entry.itemKind() == ItemEntry.ItemKind.BOOK && entry.world() == world) result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    // returns a copy of the list when called
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
            boolean builtIsBook = built instanceof EnchantBook;
            if (builtIsBook != (entry.itemKind() == ItemEntry.ItemKind.BOOK)) problems.add(entry.id() + ": entry kind is " + entry.itemKind() + " but it builds " + built.getClass().getSimpleName());
            boolean builtIsGear = built instanceof Item;
            if (builtIsGear != (entry.itemKind() == ItemEntry.ItemKind.GEAR)) problems.add(entry.id() + ": entry kind is " + entry.itemKind() + " but it builds " + built.getClass().getSimpleName());
        }
        return problems;
    }
}
