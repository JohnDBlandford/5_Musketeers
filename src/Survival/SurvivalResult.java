package Survival;

import Item.ItemNames;

// What SurvivalController.use did, so the inventory screen can show it and the caller can heal the player.
// hunger, hydration and health are the points that REALLY changed (already clamped to what fit), not the item's raw numbers.
// The controller never touches the player's HP, so the caller adds health() itself.
public record SurvivalResult(Outcome outcome, String itemName, int hunger, int hydration, int health) {
    public enum Outcome {
        USED,  // one was used up
        NOTHING_TO_RESTORE,  // it would have changed nothing, so it was kept
        NOT_FOUND  // the pack has none of that name
    }

    public SurvivalResult {
        if (outcome == null) throw new IllegalArgumentException("Outcome cannot be null");
        if (itemName == null) throw new IllegalArgumentException("Item name cannot be null");
        if (hunger < 0 || hydration < 0 || health < 0) {
            throw new IllegalArgumentException("Gains cannot be negative: " + hunger + ", " + hydration + ", " + health);
        }
    }

    // text for the inventory screen. PLACEHOLDER wording
    public String message() {
        final String name = ItemNames.getPrettyName(itemName);
        return switch (outcome) {
            case NOT_FOUND -> "You have no " + name + ".";
            case NOTHING_TO_RESTORE -> "You don't need " + name + " right now.";
            case USED -> {
                StringBuilder text = new StringBuilder("Used " + name + ".");
                if (hunger > 0) text.append(" Hunger +").append(hunger);
                if (hydration > 0) text.append(" Hydration +").append(hydration);
                if (health > 0) text.append(" Health +").append(health);
                yield text.toString();
            }
        };
    }
}
