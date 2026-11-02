package Shop.Merchant;

import Item.Consumable.Consumable;
import Item.Item;
import Item.ItemEntry;
import Item.ItemEntry.ItemKind;

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
        Objects.requireNonNull(gearPool, "gearPool");
        Objects.requireNonNull(consumablePool, "consumablePool");
        Objects.requireNonNull(random, "random");
        if (shopWorld < ItemEntry.getMinWorld() || shopWorld > ItemEntry.getMaxWorld()) {
            throw new IllegalArgumentException("World must be between " + ItemEntry.getMinWorld() + "-"
                    + ItemEntry.getMaxWorld() + ", but got: " + shopWorld);
        }
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
            Item.ItemRarity rarity = rollRarity(shopWorld, buckets, random);
            if (rarity == null) break; // every bucket is empty, the shop is just smaller
            ItemEntry entry = pickWithinRarity(shopWorld, buckets.get(rarity), random);
            slots.add(new ShopSlot(entry, MerchantConfig.getGearPrice(entry.rarity(), entry.world()),
                    MerchantConfig.getStockPerSlot(entry.rarity()), false));
        }
        return slots;
    }

    // picks a rarity that still has items. rarities with no items left are skipped and the weights of the rest are
    // re-normalised. if none of the rarities left has any weight (like only Commons left in world 3), they are picked evenly
    // so the slot is still filled. returns null if nothing is left
    private static Item.ItemRarity rollRarity(int shopWorld, Map<Item.ItemRarity, List<ItemEntry>> buckets, Random random) {
        List<Item.ItemRarity> available = new ArrayList<>();
        int totalWeight = 0;
        for (Item.ItemRarity rarity : Item.ItemRarity.values()) {
            if (buckets.get(rarity).isEmpty()) continue;
            available.add(rarity);
            totalWeight += MerchantConfig.getRarityWeight(shopWorld, rarity);
        }
        if (available.isEmpty()) return null;
        if (totalWeight == 0) return available.get(random.nextInt(available.size()));

        int roll = random.nextInt(totalWeight);
        for (Item.ItemRarity rarity : available) {
            roll -= MerchantConfig.getRarityWeight(shopWorld, rarity);
            if (roll < 0) return rarity;
        }
        throw new IllegalStateException("Rarity roll fell off the end"); // cannot happen, the weights add up to totalWeight. here just
        // because its a catch in case something weird happens
    }

    // removes and returns one entry. items from the shop's own world are CURRENT_WORLD_WEIGHT times as likely to be shown
    private static ItemEntry pickWithinRarity(int shopWorld, List<ItemEntry> bucket, Random random) {
        int totalWeight = 0;
        for (ItemEntry entry : bucket) totalWeight += weightOf(shopWorld, entry);
        int roll = random.nextInt(totalWeight);
        for (int i = 0; i < bucket.size(); i++) {
            roll -= weightOf(shopWorld, bucket.get(i));
            if (roll < 0) return bucket.remove(i);
        }
        throw new IllegalStateException("Item roll fell off the end"); // cannot happen but here just in case
    }

    // returns the weight of the item in the current world
    private static int weightOf(int shopWorld, ItemEntry entry) {
        return entry.world() == shopWorld ? MerchantConfig.CURRENT_WORLD_WEIGHT : 1;
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
