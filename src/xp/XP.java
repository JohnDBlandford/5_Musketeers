package xp;

import java.util.Objects;
import java.util.function.IntToLongFunction;

// Tracks one thing's level and xp. Make one instance per thing that levels (one per item, one per player), never
// a shared one, because the xp inside is that thing's own

// The curve (xp needed to leave each level) and the max level are passed in, so items and the player can level
// differently while sharing the same level-up logic. This class only counts. It doesn't know what a level is
// worth. The owner finds out by calling addXP and checking how many levels came back (or by comparing getLevel()
// before and after), then applies its own rewards
public class XP {
    private final int maxLvl;
    private final IntToLongFunction xpToNextLvl;
    private int startLvl = 1;
    private long xp = 0; // xp earned inside the current level, 0 at max level

    public XP(int maxLvl, IntToLongFunction xpToNextLvl) {
        // disallow maxLvl to be less than 1
        if (maxLvl < 1) throw new IllegalArgumentException("Max level must be at least 1, but got: " + maxLvl);
        this.maxLvl = maxLvl;
        this.xpToNextLvl = Objects.requireNonNull(xpToNextLvl, "xp to next level");
    }

    // player curve: EXP_to_next(L) = L^3. use as new XP(maxLevel, XP::cubicCurve)
    public static long cubicCurve(int lvl) { return (long) lvl * lvl * lvl; }

    public int getLvl() { return startLvl;}
    public int getMaxLvl() { return maxLvl; }
    public boolean isMaxLvl() { return startLvl >= maxLvl;}

    // xp earned toward the next level
    public long getCurrXP() { return xp; }

    // xp needed to get from the current level to the next one, 0 once max level
    public long getXpToNextLvl() { return isMaxLvl() ? 0 : xpRequiredToLeaveLvl(startLvl); }

    // how full a xp bar should be - max lvl is always full
    public double getXpFraction() { return isMaxLvl() ? 1.0 : (double) xp / getXpToNextLvl(); }

    // adds xp and returns how many levels were gained. one grant can cross several levels
    // zero or negative xp is ignored
    public int addXP(long amount) {
        if (amount <= 0 || isMaxLvl()) return 0;
        xp = (amount > Long.MAX_VALUE - xp) ? Long.MAX_VALUE : xp + amount;   // a huge grant can't wrap around
        int gainedXP = 0;
        while (!isMaxLvl() && xp >= xpRequiredToLeaveLvl(startLvl)) {
            xp -= xpRequiredToLeaveLvl(startLvl);
            startLvl++;
            gainedXP++;
        }
        if (isMaxLvl()) xp = 0;
        return gainedXP;
    }

    private long xpRequiredToLeaveLvl(int fromLevel) {
        long required = xpToNextLvl.applyAsLong(fromLevel);
        // cant have zero or less xp
        if (required <= 0) {
            throw new IllegalStateException("xp curve must be positive, but level " + fromLevel + " needs " + required);
        }
        return required;
    }

    @Override
    public String toString() {
        return isMaxLvl() ? "Lv " + startLvl + " (MAX)" : "Lv " + maxLvl + " (" + xp + "/" + getXpToNextLvl() + " XP)";
    }
}
