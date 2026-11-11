package Item.Registry;

import Item.Item.ItemRarity;
import Item.ItemEntry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

// The rarity roll and the weighted item pick that the Merchant and the chests share, so neither one has its own copy.
// This is the rolling code that used to live inside MerchantStock. Stateless: every method takes the Random it should use,
// so a seeded Random gives a repeatable result.
//
// Rarity weights come from the design doc (4.12). LCK raises the Epic and Legendary weights by 1% per point
// (weight * (1 + luck / 100)) before the weights are re-normalised, so luck can never make a Common or Uncommon more
// likely and can never give weight to a rarity that has none. The Merchant always passes 0 luck, chests pass the player's.
public final class LootRoller {
    private LootRoller() {}

    // how much more likely an item from the roller's own world is to be picked than an item from an older world (PLACEHOLDER)
    public static final int CURRENT_WORLD_WEIGHT = 4;

    // base loot weight (out of 100) of a rarity in the given world, from the design doc.
    // worlds 1-2 offer everything, world 3 and the Final world only Epic and Legendary
    public static int getRarityWeight(int world, ItemRarity rarity) {
        ItemEntry.requireValidWorld(world);
        boolean lateGame = world >= 3;
        return switch (Objects.requireNonNull(rarity, "rarity weight cannot be found")) {
            case COMMON -> lateGame ? 0 : 55;
            case UNCOMMON -> lateGame ? 0 : 25;
            case EPIC -> lateGame ? 60 : 17;
            case LEGENDARY -> lateGame ? 40 : 3;
        };
    }

    // 1 + 1% per luck point (design doc 4.2 and 4.12). luck can't be negative
    public static double getLuckMultiplier(int luck) {
        if (luck < 0) throw new IllegalArgumentException("Luck cannot be negative: " + luck);
        return 1.0 + luck / 100.0;
    }

    // the base weight with luck applied. only Epic and Legendary are boosted
    public static double getEffectiveWeight(int world, ItemRarity rarity, int luck) {
        double multiplier = getLuckMultiplier(luck);
        int base = getRarityWeight(world, rarity);
        return (rarity == ItemRarity.EPIC || rarity == ItemRarity.LEGENDARY) ? base * multiplier : base;
    }

    // picks a rarity from `available`. rarities that are not available are skipped and the weights of the rest are
    // re-normalised. if none of the rarities left has any weight (like only Commons left in world 3), they are picked
    // evenly so the slot is still filled. returns null if nothing is available.
    // walks ItemRarity.values() instead of the set, so the order the set happens to iterate in never changes the result
    public static ItemRarity rollRarity(int world, Set<ItemRarity> available, int luck, Random random) {
        Objects.requireNonNull(available, "available not set");
        Objects.requireNonNull(random, "random not set");
        ItemEntry.requireValidWorld(world);
        getLuckMultiplier(luck); // rejects negative luck even when nothing is available

        List<ItemRarity> candidates = new ArrayList<>();
        double totalWeight = 0;
        for (ItemRarity rarity : ItemRarity.values()) {
            if (!available.contains(rarity)) continue;
            candidates.add(rarity);
            totalWeight += getEffectiveWeight(world, rarity, luck);
        }
        if (candidates.isEmpty()) return null;
        if (totalWeight == 0) return candidates.get(random.nextInt(candidates.size()));

        double roll = random.nextDouble() * totalWeight;
        for (ItemRarity rarity : candidates) {
            roll -= getEffectiveWeight(world, rarity, luck);
            if (roll < 0) return rarity;
        }
        // only reachable through floating point rounding at the very top of the range. the last candidate is the
        // one the roll belongs to, and a candidate with weight 0 can't be last-with-weight unless all are 0 (handled above)
        return candidates.getLast();
    }

    // removes and returns one entry from the bucket. items from the roller's own world are CURRENT_WORLD_WEIGHT times
    // as likely as older ones. the bucket must not be empty
    public static ItemEntry pickWithinRarity(int world, List<ItemEntry> bucket, Random random) {
        Objects.requireNonNull(bucket, "bucket not set");
        Objects.requireNonNull(random, "random not set");
        ItemEntry.requireValidWorld(world);
        if (bucket.isEmpty()) throw new IllegalArgumentException("Cannot pick from an empty bucket");

        int totalWeight = 0;
        for (ItemEntry entry : bucket) totalWeight += weightOf(world, entry);
        int roll = random.nextInt(totalWeight);
        for (int i = 0; i < bucket.size(); i++) {
            roll -= weightOf(world, bucket.get(i));
            if (roll < 0) return bucket.remove(i);
        }
        throw new IllegalStateException("Item roll fell off the end"); // cannot happen, here just in case
    }

    // rolls one item from a pool: a rarity first (only rarities the pool has), then an item within it. the pool itself is
    // not changed, so the same pool can be rolled again. returns null for an empty pool
    public static ItemEntry rollOne(int world, List<ItemEntry> pool, int luck, Random random) {
        Objects.requireNonNull(pool, "pool not set");
        Objects.requireNonNull(random, "random not set");
        ItemEntry.requireValidWorld(world);
        getLuckMultiplier(luck);

        Map<ItemRarity, List<ItemEntry>> buckets = new EnumMap<>(ItemRarity.class);
        for (ItemEntry entry : pool) buckets.computeIfAbsent(entry.rarity(), rarity -> new ArrayList<>()).add(entry);
        if (buckets.isEmpty()) return null;

        ItemRarity rarity = rollRarity(world, EnumSet.copyOf(buckets.keySet()), luck, random);
        return pickWithinRarity(world, buckets.get(rarity), random);
    }

    private static int weightOf(int world, ItemEntry entry) {
        return entry.world() == world ? CURRENT_WORLD_WEIGHT : 1;
    }
}
