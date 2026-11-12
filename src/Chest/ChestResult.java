package Chest;

import Item.Item.ItemRarity;
import Item.ItemNames;

import java.util.List;

// What opening a chest did, for the textbox (chest spec, section 1).
//  - GOLD_ONLY: the chest held no item
//  - ITEM_ADDED: the item is now in the pack
//  - ITEM_LOST: the pack had no room, so the item was destroyed. the gold was still paid and the result names the item
//  - ALREADY_OPENED: the chest was opened before. nothing was paid or given
// itemName and rarity are only set for ITEM_ADDED and ITEM_LOST (rarity comes from the registry entry that was rolled)
public record ChestResult(Outcome outcome, int gold, String itemName, ItemRarity rarity) {
    public enum Outcome { GOLD_ONLY, ITEM_ADDED, ITEM_LOST, ALREADY_OPENED }

    /**
     * @author Derek
     * @param outcome the outcome of the chest once its been opened
     * @param gold the amount of gold found
     * @param itemName the item name found (if an item drops)
     * @param rarity the rarity level of the item if it drops
     */
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

    // the text for the textbox, one string per page (the engine Textbox is 30pt Arial in a 750px box, so keep a page under about
    // 52 characters). PLACEHOLDER wording. the item's name is spaced out ("WraithboneSaber" -> "Wraithbone Saber")

    /**
     * @author Derek
     * @return returns a list based off of outcome case
     */
    public List<String> pages() {
        String goldPage = "You found " + gold + " gold.";
        return switch (outcome) {
            case ALREADY_OPENED -> List.of("The chest is empty.");
            case GOLD_ONLY -> List.of(goldPage);
            case ITEM_ADDED -> List.of(goldPage, "You also found: " + ItemNames.getPrettyName(itemName) + ".");
            case ITEM_LOST -> List.of(goldPage, "Pack full! " + ItemNames.getPrettyName(itemName) + " was lost.");
        };
    }

    // all the pages as one line

    /**
     * @author Derek
     * @return returns the formated msg
     */
    public String message() { return String.join(" ", pages()); }
}
