package Item;

// A single use item (food, a water bottle, ...). Consumables are stateless: no durability, level or xp, so the inventory
// stacks them by name instead of tracking individual instances.
// The restore values are raw amounts. Clamping to the 100 max and applying them is the hunger/hydration/health systems' job.
public interface Consumable extends Stockable {
    int getHungerRestore();
    int getHydrationRestore();
    int getHealthRestore();
}
