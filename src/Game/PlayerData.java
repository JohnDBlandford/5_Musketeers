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
        if (currency >= amount) {
            currency -= amount;
        }
    }
}