package Shop.Merchant;

import Item.Consumable.Consumable;
import Item.Item;
import Item.ItemEntry;
import Item.ItemEntry.ItemKind;
import Item.Registry.LootRoller;

import java.util.*;

// One world's shop stock: the gear slots plus one food slot and one water slot.
// Built by rollStock(), which does all of the random picking
public final class MerchantStock {
    private final int shopWorld;
    private final List<ShopSlot> gearSlots;
    private final ShopSlot foodSlot;   // null if the pool had no food
    private final ShopSlot waterSlot;  // null if the pool had no water

    private MerchantStock(int shopWorld, List<ShopSlot> gearSlots, ShopSlot foodSlot, ShopSlot waterSlot) {
        this.shopWorld = shopWorld;
        this.gearSlots = Collections.unmodifiableList(gearSlots);
        this.foodSlot = foodSlot;
        this.waterSlot = waterSlot;
    }

    public int getShopWorld() { return this.shopWorld; }
    // in the order they were drawn
    public List<ShopSlot> getGearSlots() { return this.gearSlots; }
    public Optional<ShopSlot> getFoodSlot() { return Optional.ofNullable(foodSlot); }
    public Optional<ShopSlot> getWaterSlot() { return Optional.ofNullable(waterSlot); }

    // Rolls a new stock. gearPool is every gear entry this shop is allowed to sell and consumablePool every consumable.
    // The caller decides what is eligible (worlds 1-3 see their world and older, the Final world only its own), this
    // class only does the picking. All randomness comes from `random`, so a seeded Random gives a repeatable shop.
    public static MerchantStock rollStock(int shopWorld, List<ItemEntry> gearPool, List<ItemEntry> consumablePool, Random random) {
        Objects.requireNonNull(gearPool, "gearPool missing");
        Objects.requireNonNull(consumablePool, "consumablePool missing");
        Objects.requireNonNull(random, "random not set");
        ItemEntry.requireValidWorld(shopWorld);
        for (ItemEntry entry : gearPool) {
            if (entry.itemKind() != ItemKind.GEAR) throw new IllegalArgumentException(entry.id() + " is not gear");
        }
        for (ItemEntry entry : consumablePool) {
            if (entry.itemKind() != ItemKind.CONSUMABLE) throw new IllegalArgumentException(entry.id() + " is not a consumable");
        }

        List<ShopSlot> gear = rollGear(shopWorld, gearPool, random);

        // food then water, always in that order so a seed always gives the same shop
        List<ItemEntry> foods = new ArrayList<>();
        List<ItemEntry> waters = new ArrayList<>();
        for (ItemEntry entry : consumablePool) {
            if (!(entry.create() instanceof Consumable consumable)) {
                throw new IllegalArgumentException(entry.id() + " does not build a Consumable");
            }
            if (consumable.getHydrationRestore() > 0) waters.add(entry);
            else if (consumable.getHungerRestore() > 0) foods.add(entry);
            // anything that only restores health has no guaranteed slot
        }
        return new MerchantStock(shopWorld, gear, pickConsumable(foods, random), pickConsumable(waters, random));
    }

    private static ShopSlot pickConsumable(List<ItemEntry> candidates, Random random) {
        if (candidates.isEmpty()) return null;
        ItemEntry entry = candidates.get(random.nextInt(candidates.size()));
        return new ShopSlot(entry, MerchantConfig.getConsumablePrice(entry.id()), 0, true);
    }

    // draws up to GEAR_SLOTS items without replacement. each draw rolls a rarity, then an item within that rarity
    private static List<ShopSlot> rollGear(int shopWorld, List<ItemEntry> gearPool, Random random) {
        Map<Item.ItemRarity, List<ItemEntry>> buckets = new EnumMap<>(Item.ItemRarity.class);
        for (Item.ItemRarity rarity : Item.ItemRarity.values()) buckets.put(rarity, new ArrayList<>());
        for (ItemEntry entry : gearPool) buckets.get(entry.rarity()).add(entry);

        List<ShopSlot> slots = new ArrayList<>();
        for (int i = 0; i < MerchantConfig.GEAR_SLOTS; i++) {
            // the shop never uses luck, so it always rolls with 0
            Item.ItemRarity rarity = LootRoller.rollRarity(shopWorld, availableRarities(buckets), 0, random);
            if (rarity == null) break; // every bucket is empty, the shop is just smaller
            ItemEntry entry = LootRoller.pickWithinRarity(shopWorld, buckets.get(rarity), random);
            slots.add(new ShopSlot(entry, MerchantConfig.getGearPrice(entry.rarity(), entry.world()),
                    MerchantConfig.getStockPerSlot(entry.rarity()), false));
        }
        return slots;
    }

    // the rarities that still have items left, which is what LootRoller is allowed to roll
    private static Set<Item.ItemRarity> availableRarities(Map<Item.ItemRarity, List<ItemEntry>> buckets) {
        Set<Item.ItemRarity> available = EnumSet.noneOf(Item.ItemRarity.class);
        for (Map.Entry<Item.ItemRarity, List<ItemEntry>> bucket : buckets.entrySet()) {
            if (!bucket.getValue().isEmpty()) available.add(bucket.getKey());
        }
        return available;
    }

    // one thing on sale. gear has a limited stock, food and water are unlimited
    public static final class ShopSlot {
        private final String id;
        private final ItemKind kind;
        private final Item.ItemRarity rarity;
        private final int itemWorld;
        private final int unitPrice;
        private final boolean unlimited;
        private int remaining;

        private ShopSlot(ItemEntry entry, int unitPrice, int stock, boolean unlimited) {
            this.id = entry.id();
            this.kind = entry.itemKind();
            this.rarity = entry.rarity();
            this.itemWorld = entry.world();
            this.unitPrice = unitPrice;
            this.remaining = stock;
            this.unlimited = unlimited;
        }

        public String getId() { return this.id; }
        public ItemKind getKind() { return this.kind; }
        public Item.ItemRarity getRarity() { return this.rarity; }
        // the world the ITEM belongs to, which can be older than the world of the shop selling it
        public int getItemWorld() { return this.itemWorld; }
        public int getUnitPrice() { return this.unitPrice; }
        public boolean isUnlimited() { return this.unlimited; }
        // Integer.MAX_VALUE for an unlimited slot
        public int getRemainingStock() { return unlimited ? Integer.MAX_VALUE : remaining; }
        public boolean isSoldOut() { return !unlimited && remaining <= 0; }

        // only the merchant sells things, the screen never touches stock. the merchant checks quantity first
        void take(int quantity) {
            if (unlimited) return;
            if (quantity < 1 || quantity > remaining) throw new IllegalArgumentException("Cannot take " + quantity + " of " + id);
            remaining -= quantity;
        }
    }
}
