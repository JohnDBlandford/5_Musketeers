package Survival;

// One survival value (hunger or hydration) from 0 to MAX. It starts full, drains toward 0 and is refilled by eating or drinking.
// Pure logic, so it can be tested without the game running. Which meter it is, and how fast it drains, is the controller's business.
public final class Meter implements ISurvival {
    private static final int MAX = 100;

    private int value = MAX;

    public Meter() {}

    public int getValue() { return value; }
    public static int getMaxMeter() { return MAX; }

    // true only at exactly 0
    public boolean isEmpty() { return value == 0; }

    // lowers the value, never below 0. 0 is allowed and does nothing
    public void drain(final int amount) {
        if (amount < 0) throw new IllegalArgumentException("Cannot drain a negative amount: " + amount);
        value = Math.max(0, value - amount);
    }

    // raises the value, never above MAX, and returns how many points were really added (so a full meter returns 0)
    public int restore(final int points) {
        if (points < 0) throw new IllegalArgumentException("Cannot restore a negative amount: " + points);
        int added = Math.min(points, MAX - value);
        value += added;
        return added;
    }

    @Override
    public void reset() { value = MAX; }
}
