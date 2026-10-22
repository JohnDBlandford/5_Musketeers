package BattleSystem;

import Item.Item;

// One move the player can pick in the ability window.
// power is a multiplier on the attack (1.0 = 100%). Your items store 0.0 for every ability right now, so the real numbers live here.
public final class Ability {
    public enum Effect {
        STRIKE,       // plain damage
        BURST,        // one big hit
        LIFESTEAL,    // heals the user for half the damage dealt
        FINISHER,     // double damage if the target is under 30% HP
        MULTI_HIT,    // three weaker hits
        ATK_UP,       // damage, then raises the user's ATK
        SPD_UP,       // damage, then raises the user's SPD
        DEF_DOWN,     // damage, then lowers the target's DEF
        PIERCE,       // ignores half of the target's DEF
        STUN,         // damage with a chance to make the target skip its next turn
        GUARD,        // the next hit taken is blocked
        COUNTER,      // counter-attacks the next hit taken
        THROW         // damage based on the shield's block amount
    }

    private final Item.ItemAbilities key;
    private final String name;
    private final MoveCategory category;
    private final double power;
    private final double accuracy;   // the move's base chance to land (1.0 = 100%), before the weapon's ACCURACY buff and the enemy's dodge
    private final Effect effect;
    private final String description;

    public Ability(Item.ItemAbilities key, String name, MoveCategory category, double power, double accuracy, Effect effect, String description) {
        this.key = key;
        this.name = name;
        this.category = category;
        this.power = power;
        this.accuracy = accuracy;
        this.effect = effect;
        this.description = description;
    }

    public Item.ItemAbilities getKey() { return key; }
    public String getName() { return name; }
    public MoveCategory getCategory() { return category; }
    public double getPower() { return power; }
    public double getAccuracy() { return accuracy; }
    public Effect getEffect() { return effect; }
    public String getDescription() { return description; }

    // moves that only set a stance (GUARD, COUNTER) never roll to hit, so they show "--"
    public boolean rollsToHit() { return effect != Effect.GUARD && effect != Effect.COUNTER; }

    public String getAccuracyText() {
        return rollsToHit() ? Math.round(accuracy * 100) + "%" : "--";
    }

    public String getPowerText() {
        return power <= 0 ? "--" : Math.round(power * 100) + "%";
    }
}