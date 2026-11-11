package Survival;

import Item.Consumable.Consumable;
import Survival.SurvivalResult.Outcome;
import inventory.Inventory;

import java.util.Objects;

// Owns the hunger and hydration meters and the rules that move them. Pure logic: it counts the frames it is
// told about and never touches the player or the screen, so it can be tested without the game running.
//   tick():  call once per frame, only while the game is RUNNING (so nothing drains in battles, menus, shops or the inventory).
//            Every fps * 9 frames each meter loses 1 point (100 points = 15 real minutes). Every fps * 10 frames (1 in-game minute)
//            it returns the HP the player loses: 1 for each empty meter. On every other frame it returns 0.
//   use():   uses one consumable from the pack. The caller applies the health gain in the result to the player.
// All the numbers are PLACEHOLDERS until the hunger/hydration numbers are balanced.
public final class SurvivalController implements ISurvival {
    private static final int REAL_SECONDS_PER_POINT = 9;
    private static final int REAL_SECONDS_PER_GAME_MINUTE = 10;   // same as GameClock

    private final long framesPerPoint;
    private final long framesPerMinute;
    private final Meter hunger = new Meter();
    private final Meter hydration = new Meter();
    // long so an extended session cannot overflow
    private long frames = 0;

    // fps (60 default, 30 in the slower mode)
    public SurvivalController(int fps) {
        assert fps > 0;
        if (fps <= 0) throw new IllegalArgumentException("fps must be positive, but got: " + fps);
        this.framesPerPoint = (long) fps * REAL_SECONDS_PER_POINT;
        this.framesPerMinute = (long) fps * REAL_SECONDS_PER_GAME_MINUTE;
    }

    // one frame forward. returns the HP the player loses on this frame (0 on most frames)
    public int tick() {
        frames++;
        if (frames % framesPerPoint == 0) {
            hunger.drain(1);
            hydration.drain(1);
        }
        // the drain runs first, so a meter that just hit 0 already counts on a minute mark that lands on the same frame
        if (frames % framesPerMinute != 0) return 0;
        return (hunger.isEmpty() ? 1 : 0) + (hydration.isEmpty() ? 1 : 0);
    }

    public int getHunger() { return hunger.getValue(); }

    public int getHydration() { return hydration.getValue(); }

    // both meters full and the frame counter back to 0 (a new game)
    @Override
    public void reset() {
        hunger.reset();
        hydration.reset();
        frames = 0;
    }

    // hp after losing this much, but never below 1: this system never kills. a player already at 1 HP or less is left exactly as is
    public static float hpAfterLoss(float hp, int loss) {
        if (loss < 0) throw new IllegalArgumentException("Loss cannot be negative: " + loss);
        if (hp <= 1.0f) return hp;
        return Math.max(1.0f, hp - loss);
    }

    // uses ONE of the named consumable. nothing is used up when it would change nothing (every stat it restores is already full)
    // or when the pack has none. The gains in the result are what really changed, clamped to what fits
    public SurvivalResult use(Inventory inventory, String itemName, float currHp, float maxHp) {
        Objects.requireNonNull(inventory, "inventory not set");
        Objects.requireNonNull(itemName, "itemName not set");
        Consumable consumable = findInPack(inventory, itemName);
        if (consumable == null) return new SurvivalResult(Outcome.NOT_FOUND, itemName, 0, 0, 0);

        int hungerGain = Math.min(consumable.getHungerRestore(), Meter.MAX - hunger.getValue());
        int hydrationGain = Math.min(consumable.getHydrationRestore(), Meter.MAX - hydration.getValue());
        int healthGain = (int) Math.min(consumable.getHealthRestore(), Math.max(0.0f, maxHp - currHp));
        if (hungerGain + hydrationGain + healthGain == 0) return new SurvivalResult(Outcome.NOTHING_TO_RESTORE, itemName, 0, 0, 0);

        inventory.useConsumable(itemName);
        hunger.restore(hungerGain);
        hydration.restore(hydrationGain);
        return new SurvivalResult(Outcome.USED, itemName, hungerGain, hydrationGain, healthGain);
    }

    // looks without taking anything out of the pack
    private static Consumable findInPack(Inventory inventory, String itemName) {
        for (Inventory.ConsumableStack stack : inventory.getAllConsumables()) {
            if (stack.consumable().getItemName().equals(itemName)) return stack.consumable();
        }
        return null;
    }
}
