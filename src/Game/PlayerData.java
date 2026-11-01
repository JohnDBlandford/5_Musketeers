package Game;

public class PlayerData {
    // Hardcoded starting amount for testing map transitions
    private static int currency = 500;

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
}
