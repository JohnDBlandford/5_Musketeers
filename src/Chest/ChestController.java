package Chest;

import Chest.ChestResult.Outcome;
import Game.PlayerData;
import Item.Consumable.Consumable;
import Item.Item;
import Item.ItemEntry;
import Item.ItemEntry.ItemKind;
import Item.Registry.ItemRegistry;
import inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntFunction;

// Opens chests. open() checks, in order: null arguments, the world, that the chest is in the registry,
// and whether it was already opened (then nothing happens and no random numbers are used). Otherwise it rolls the contents with
// ChestLoot and the player's luck, pays the gold, puts the item in the pack if it fits (a full pack destroys it), marks the
// chest opened and returns what happened. The pools are injected by function so tests can use tiny hand made pools.
public final class ChestController {
    private final ChestRegistry registry;
    private final Random random;
    private final IntFunction<List<ItemEntry>> gearPools;
    private final IntFunction<List<ItemEntry>> consumablePools;

    /**
     * @author Derek
     * @param registry the chest registry
     * @param random the random place for each chest
     * @param gearPools the different kinds of gear that chests can give
     * @param consumablePools the different kinds of consumables that chests can give
     */
    public ChestController(ChestRegistry registry, Random random, IntFunction<List<ItemEntry>> gearPools,
                           IntFunction<List<ItemEntry>> consumablePools) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be found");
        this.random = Objects.requireNonNull(random, "random not set");
        this.gearPools = Objects.requireNonNull(gearPools, "gearPools not set");
        this.consumablePools = Objects.requireNonNull(consumablePools, "consumablePools not set");
    }

    // production: the pools come from the item registry
    /**
     * @author Derek
     * @param registry the chest registry
     * @param random the random chest location for each chest
     */
    public ChestController(ChestRegistry registry, Random random) {
        this(registry, random, ChestController::registryGearPool, ItemRegistry::queryConsumablesUpToWorld);
    }

    // worlds 1-3 hold their own gear and older gear. the Final world holds only its own pool (same rule as the shop)
    /**
     * @author Derek
     * @param world the world that the registry gear comes from
     * @return returns the gear pool from the provided world
     */
    private static List<ItemEntry> registryGearPool(int world) {
        List<ItemEntry> pool = new ArrayList<>();
        for (Item.ItemRarity rarity : Item.ItemRarity.values()) {
            pool.addAll(world == ItemEntry.getMaxWorld() ? ItemRegistry.query(world, world, rarity)
                    : ItemRegistry.queryUpToWorld(world, rarity));
        }
        return pool;
    }

    /**
     * @author Derek
     * @param id the id of the chest to be opened
     * @param world the world the chest is opened in
     * @param inventory the inventory so that items can go right into it after the chest being opened
     * @return returns a new ChestResult object that contains whether its added an item or not, and the amount of gold given, the chest id, and the items rarity
     */
    public ChestResult open(ChestId id, int world, Inventory inventory) {
        Objects.requireNonNull(id, "id not found");
        Objects.requireNonNull(inventory, "inventory not set");
        ItemEntry.requireValidWorld(world);
        // if the map doesnt contain the correct chest id, throw
        if (!registry.getChests(id.mapName()).contains(id)) {
            throw new IllegalArgumentException("No such chest in the registry: " + id);
        }
        if (registry.isOpened(id)) return new ChestResult(Outcome.ALREADY_OPENED, 0, null, null);

        ChestContents contents = ChestLoot.rollContents(world, PlayerData.getLuck(), gearPools.apply(world),
                consumablePools.apply(world), random);
        PlayerData.addCurrency(contents.gold());
        registry.markOpened(id);
        if (!contents.hasItem()) return new ChestResult(Outcome.GOLD_ONLY, contents.gold(), null, null);

        ItemEntry entry = contents.item();
        boolean added = entry.itemKind() == ItemKind.GEAR ? inventory.addItem((Item) entry.create())
                : inventory.addConsumable((Consumable) entry.create());
        return new ChestResult(added ? Outcome.ITEM_ADDED : Outcome.ITEM_LOST, contents.gold(), entry.id(), entry.rarity());
    }
}
