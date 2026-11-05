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

// Opens chests (chest spec, section 1). open() checks, in order: null arguments, the world, that the chest is in the registry,
// and whether it was already opened (then nothing happens and no random numbers are used). Otherwise it rolls the contents with
// ChestLoot and the player's luck, pays the gold, puts the item in the pack if it fits (a full pack destroys it), marks the
// chest opened and returns what happened. The pools are injected by function so tests can use tiny hand made pools.
public final class ChestController {
    private final ChestRegistry registry;
    private final Random random;
    private final IntFunction<List<ItemEntry>> gearPools;
    private final IntFunction<List<ItemEntry>> consumablePools;

    public ChestController(ChestRegistry registry, Random random, IntFunction<List<ItemEntry>> gearPools,
                           IntFunction<List<ItemEntry>> consumablePools) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.random = Objects.requireNonNull(random, "random");
        this.gearPools = Objects.requireNonNull(gearPools, "gearPools");
        this.consumablePools = Objects.requireNonNull(consumablePools, "consumablePools");
    }

    // production: the pools come from the item registry
    public ChestController(ChestRegistry registry, Random random) {
        this(registry, random, ChestController::registryGearPool, ItemRegistry::queryConsumablesUpToWorld);
    }

    // worlds 1-3 hold their own gear and older gear. the Final world holds only its own pool (same rule as the shop)
    private static List<ItemEntry> registryGearPool(int world) {
        List<ItemEntry> pool = new ArrayList<>();
        for (Item.ItemRarity rarity : Item.ItemRarity.values()) {
            pool.addAll(world == ItemEntry.getMaxWorld() ? ItemRegistry.query(world, world, rarity)
                    : ItemRegistry.queryUpToWorld(world, rarity));
        }
        return pool;
    }

    public ChestResult open(ChestId id, int world, Inventory inventory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(inventory, "inventory");
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
