package Item.Consumable;

import Item.Item;

// Data only implementation of Consumable. New foods and drinks are registered as values in a WorldNItems class
// and do not need their own class file.
public final class BasicConsumable implements Consumable {
    private final String itemName;
    private final Item.ItemRarity itemRarity;
    private final int hungerRestore;
    private final int hydrationRestore;
    private final int healthRestore;

    private BasicConsumable(String itemName, Item.ItemRarity itemRarity, int hungerRestore, int hydrationRestore, int healthRestore) {
        // make sure as much can fail at compile time rather than runtime
        if (itemName == null || itemName.isBlank()) throw new IllegalArgumentException("Consumable name cannot be blank");
        if (itemRarity == null) throw new IllegalArgumentException("Rarity cannot be null for " + itemName);
        if (hungerRestore < 0 || hydrationRestore < 0 || healthRestore < 0) {
            throw new IllegalArgumentException("Restore amounts cannot be negative for " + itemName);
        }
        if (hungerRestore + hydrationRestore + healthRestore == 0) {
            throw new IllegalArgumentException(itemName + " must restore something");
        }
        this.itemName = itemName;
        this.itemRarity = itemRarity;
        this.hungerRestore = hungerRestore;
        this.hydrationRestore = hydrationRestore;
        this.healthRestore = healthRestore;
    }

    public static BasicConsumable food(String itemName, Item.ItemRarity itemRarity, int hungerRestore) {
        return new BasicConsumable(itemName, itemRarity, hungerRestore, 0, 0);
    }

    public static BasicConsumable water(String itemName, Item.ItemRarity itemRarity, int hydrationRestore) {
        return new BasicConsumable(itemName, itemRarity, 0, hydrationRestore, 0);
    }

    // for anything that restores more than one thing
    public static BasicConsumable of(String itemName, Item.ItemRarity itemRarity, int hungerRestore, int hydrationRestore, int healthRestore) {
        return new BasicConsumable(itemName, itemRarity, hungerRestore, hydrationRestore, healthRestore);
    }

    @Override public String getItemName() { return itemName; }
    @Override public Item.ItemRarity getItemRarity() { return itemRarity; }
    @Override public int getHungerRestore() { return hungerRestore; }
    @Override public int getHydrationRestore() { return hydrationRestore; }
    @Override public int getHealthRestore() { return healthRestore; }
}
