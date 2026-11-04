package Chest;

import Item.Item.ItemRarity;

// What opening a chest did, for the textbox (chest spec, section 1).
//  - GOLD_ONLY: the chest held no item
//  - ITEM_ADDED: the item is now in the pack
//  - ITEM_LOST: the pack had no room, so the item was destroyed. the gold was still paid and the result names the item
//  - ALREADY_OPENED: the chest was opened before. nothing was paid or given
// itemName and rarity are only set for ITEM_ADDED and ITEM_LOST (rarity comes from the registry entry that was rolled)
public record ChestResult(Outcome outcome, int gold, String itemName, ItemRarity rarity) {
    public enum Outcome { GOLD_ONLY, ITEM_ADDED, ITEM_LOST, ALREADY_OPENED }

    public ChestResult {
        if (outcome == null) throw new IllegalArgumentException("Outcome cannot be null");
        if (gold < 0) throw new IllegalArgumentException("Gold cannot be negative, but got: " + gold);
        boolean hasItem = outcome == Outcome.ITEM_ADDED || outcome == Outcome.ITEM_LOST;
        if (hasItem) {
            if (itemName == null || itemName.isBlank()) throw new IllegalArgumentException(outcome + " needs an item name");
            if (rarity == null) throw new IllegalArgumentException(outcome + " needs a rarity");
        } else if (itemName != null || rarity != null) {
            throw new IllegalArgumentException(outcome + " cannot carry an item");
        }
        if (outcome == Outcome.ALREADY_OPENED && gold != 0) {
            throw new IllegalArgumentException("An already opened chest pays no gold, but got: " + gold);
        }
    }

    // the text for the textbox. PLACEHOLDER wording
    public String message() {
        return switch (outcome) {
            case ALREADY_OPENED -> "The chest is empty.";
            case GOLD_ONLY -> "You found " + gold + " gold.";
            case ITEM_ADDED -> "You found " + gold + " gold and a " + itemName + ".";
            case ITEM_LOST -> "You found " + gold + " gold. Your pack is full, so the " + itemName + " was lost.";
        };
    }
}
