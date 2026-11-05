package Shop.Merchant;

import Game.PlayerData;
import Item.Consumable.Consumable;
import Item.Item;
import Item.ItemEntry;
import Item.ItemEntry.ItemKind;
import Item.Registry.ItemRegistry;
import Shop.GameClock;
import Shop.PurchaseResult;
import inventory.Inventory;

import java.util.*;

// Sells gear and food to the player. Keeps a separate MerchantStock for each world and re-rolls a world's stock once
// RESTOCK_INTERVAL_MINUTES of in-game time have passed since it was last rolled. The screen only sees this through IMerchant.
public class Merchant implements IMerchant {
    private final GameClock clock;
    private final Random random;
    private final Map<Integer, MerchantStock> stocks = new HashMap<>();
    // the clock's frame count when each world was last rolled
    private final Map<Integer, Long> lastRestockFrame = new HashMap<>();

    // random is passed in so tests can seed it
    public Merchant(GameClock clock, Random random) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public void restockIfDue(int world) {
        ItemEntry.requireValidWorld(world);
        Long last = lastRestockFrame.get(world);
        long intervalFrames = (long) MerchantConfig.RESTOCK_INTERVAL_MINUTES * clock.getFramesPerGameMinute();
        if (last != null && clock.getFrames() - last < intervalFrames) return;

        stocks.put(world, MerchantStock.rollStock(world, gearPool(world), ItemRegistry.queryConsumablesUpToWorld(world), random));
        // the timer restarts now, so a long absence causes one restock and not a backlog of them
        lastRestockFrame.put(world, clock.getFrames());
    }

    @Override
    public List<SlotView> getSlots(int world) {
        List<SlotView> views = new ArrayList<>();
        for (MerchantStock.ShopSlot slot : getSlotsOf(world)) views.add(getViewOf(slot));
        return Collections.unmodifiableList(views);
    }

    @Override
    public PurchaseResult purchase(int world, int slotIndex, int quantity, Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        List<MerchantStock.ShopSlot> slots = getSlotsOf(world);
        if (slotIndex < 0 || slotIndex >= slots.size()) throw new IllegalArgumentException("No slot at index " + slotIndex);
        MerchantStock.ShopSlot slot = slots.get(slotIndex);

        // every check happens before anything changes, so a rejected purchase costs the player nothing
        if (slot.isSoldOut()) return PurchaseResult.SOLD_OUT;
        if (quantity < 1 || quantity > MerchantConfig.MAX_PER_PURCHASE || quantity > slot.getRemainingStock()) {
            return PurchaseResult.OVER_LIMIT;
        }

        int total = slot.getUnitPrice() * quantity;
        if (PlayerData.getCurrency() < total) return PurchaseResult.NOT_ENOUGH_GOLD;

        boolean isGear = slot.getKind() == ItemKind.GEAR;
        // consumables are shared constants, gear is built fresh per unit so every copy has its own durability
        Consumable consumable = isGear ? null : ItemRegistry.createConsumable(slot.getId());
        boolean hasRoom = isGear ? inventory.canAddItems(quantity) : inventory.canAddConsumables(consumable, quantity);
        if (!hasRoom) return PurchaseResult.INVENTORY_FULL;

        // all checks passed, nothing below should be able to fail
        if (!PlayerData.trySpend(total)) throw new IllegalStateException("Gold changed during a purchase");
        for (int i = 0; i < quantity; i++) {
            boolean added = isGear ? inventory.addItem(ItemRegistry.createItem(slot.getId())) : inventory.addConsumable(consumable);
            if (!added) throw new IllegalStateException("Inventory refused " + slot.getId() + " after reporting room for it");
        }
        slot.take(quantity);
        return PurchaseResult.SUCCESS;
    }

    // gear, then food, then water. a slot's position here is the index purchase() takes
    private List<MerchantStock.ShopSlot> getSlotsOf(int world) {
        ItemEntry.requireValidWorld(world);
        MerchantStock stock = stocks.get(world);
        if (stock == null) throw new IllegalStateException("World " + world + " has not been stocked, call restockIfDue first");
        List<MerchantStock.ShopSlot> slots = new ArrayList<>(stock.getGearSlots());
        stock.getFoodSlot().ifPresent(slots::add);
        stock.getWaterSlot().ifPresent(slots::add);
        return slots;
    }

    // constructs a new SlotView object to display
    private static SlotView getViewOf(MerchantStock.ShopSlot slot) {
        return new SlotView(slot.getId(), slot.getKind(), slot.getRarity(), slot.getItemWorld(), slot.getUnitPrice(),
                slot.getRemainingStock(), slot.isUnlimited());
    }

    // worlds 1-3 sell their own gear and older gear. the Final world sells only its own pool
    private static List<ItemEntry> gearPool(int world) {
        List<ItemEntry> pool = new ArrayList<>();
        for (Item.ItemRarity rarity : Item.ItemRarity.values()) {
            pool.addAll(world == ItemEntry.getMaxWorld() ? ItemRegistry.query(world, world, rarity)
                    : ItemRegistry.queryUpToWorld(world, rarity));
        }
        return pool;
    }
}
