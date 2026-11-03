package Game;

public class PlayerData {
    // Hardcoded starting amount for testing map transitions
    private static int currency = 500;

    // the player's LCK stat (design doc 4.2): each point adds 1% to the Epic and Legendary loot weights. chests read it.
    // nothing raises it yet, so it stays 0 until skill points exist
    private static int luck = 0;

    public static int getCurrency() {
        return currency;
    }

    public static void addCurrency(int amount) {
        currency += amount;
    }

    public static void spendCurrency(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Cannot spend a negative amount: " + amount);
        if (currency >= amount) {
            currency -= amount;
        }
    }

    // all or nothing: takes the full amount and returns true, or changes nothing and returns false if the player is short.
    // shops use this after they have checked stock and inventory room, so a failed purchase never costs gold
    public static boolean trySpend(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Cannot spend a negative amount: " + amount);
        if (currency < amount) return false;
        currency -= amount;
        return true;
    }

    public static int getLuck() {
        return luck;
    }

    // a negative luck would shrink the Epic and Legendary weights below their base, so it is rejected and the old value is kept
    public static void setLuck(int value) {
        if (value < 0) throw new IllegalArgumentException("Luck cannot be negative: " + value);
        luck = value;
    }
}
