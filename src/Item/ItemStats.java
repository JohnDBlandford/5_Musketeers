package Item;

import xp.XP;

import java.util.*;

// Per-item level, XP and durability, plus the scaling rules that turn an item's level 1 values into its
// current values. Every Item owns exactly one of these, so two copies of the same sword track their own state.
public final class ItemStats {
    public static final int MAX_LVL = 100;

    // a buff or ability can never be worth more than 6 times its base stat buff (its lvl one value)
    public static final double MAX_SCALE = 6.0;

    // cap on chance type buffs since a chance type buff at 100% is meaningless
    public static final double CHANCE_CEILING = 0.95;

    // chance buffs are stored here inside a unmodifiable set, these have the CHANCE_CEILING applied to them
    private static final Set<Item.ItemBuffs> CHANCE_BUFFS = Collections.unmodifiableSet(EnumSet.of(
            Item.ItemBuffs.CRIT_CHANCE, Item.ItemBuffs.ACCURACY,
            Item.ItemBuffs.DMG_REDUCTION, Item.ItemBuffs.PIERCING));

    private static final double EPSILON = 1e-9;

    // XP formula needs to be level L to L + 1 is XP_BASE * L ^ XP_EXPONENT
    private static final double XP_BASE  = 100.0;
    private static final double XP_EXPO = 1.5;

    // growth rate formula -> scaled value = level 1 value * (1 + growth * (level - 1))
    private static final double DEFAULT_ATTACK_GROWTH = 0.03;
    private static final double DEFAULT_DEFENSE_GROWTH = 0.03;
    private static final double DEFAULT_BUFF_GROWTH = 0.02;
    private static final double DEFAULT_ABILITY_GROWTH = 0.02;
    private static final double DEFAULT_DURABILITY_GROWTH = 0.02;

    // fixed per item stats
    private final int baseMaxDurability;
    private final double attackGrowth;
    private final double defenseGrowth;
    private final double buffGrowth;
    private final double abilityGrowth;
    private final double durabilityGrowth;
    // this map lets one ability scale differently from the rest
    private final Map<Item.ItemAbilities, Double> abilityGrowthOverrides;

    // changes over the life of the item - lvl and xp are a part of the XP class
    // durability modifier is only for items to track when they will break
    private final XP progress = new XP(MAX_LVL, ItemStats::xpRequiredToLeaveLvl);
    private int itemDurability;

    public ItemStats(int baseMaxDurability, double attackGrowth, double defenseGrowth, double buffGrowth,
                     double abilityGrowth, double durabilityGrowth, Map<Item.ItemAbilities, Double> abilityGrowthOverrides) {
        // disallow baseMaxDurability be less than or equal to 0
        if (baseMaxDurability <= 0) {
            throw new IllegalArgumentException("Base max durability must be positive, but got: " + baseMaxDurability);
        }
        // disallow negative growth rate values
        if (attackGrowth < 0 || defenseGrowth < 0 || buffGrowth < 0 || abilityGrowth < 0 || durabilityGrowth < 0) {
            throw new IllegalArgumentException("Growth rates cannot be negative");
        }
        this.baseMaxDurability = baseMaxDurability;
        this.attackGrowth = attackGrowth;
        this.defenseGrowth = defenseGrowth;
        this.buffGrowth = buffGrowth;
        this.abilityGrowth = abilityGrowth;
        this.durabilityGrowth = durabilityGrowth;
        this.abilityGrowthOverrides = abilityGrowthOverrides.isEmpty() ? Map.of()
                : Collections.unmodifiableMap(new EnumMap<>(abilityGrowthOverrides));
        this.itemDurability = baseMaxDurability;
    }

    // constructor for initializing the default values
    public ItemStats(int baseMaxDurability) {
        this(baseMaxDurability, DEFAULT_ATTACK_GROWTH, DEFAULT_DEFENSE_GROWTH, DEFAULT_BUFF_GROWTH,
                DEFAULT_ABILITY_GROWTH, DEFAULT_DURABILITY_GROWTH, Map.of());
    }

    // gets the default for any item that does not set its own
    // these are placeholders for now before testing
    public static ItemStats getDefaultsForItemRarity(Item.ItemRarity itemRarity) {
        return new ItemStats(switch (itemRarity) {
            case COMMON -> 50;
            case UNCOMMON -> 75;
            case EPIC -> 110;
            case LEGENDARY -> 160;
        });
    }

    // the counting of the xp, lvls, and max lvl is done by the XP class
    // item specific is the curve and what happens with the durability
    public int getLvl() { return progress.getLvl(); }
    public boolean isMaxLvl() { return progress.isMaxLvl(); }
    public long getXp() { return progress.getCurrXP(); }

    // xp needed to go from one lvl to the next
    public static long xpRequiredToLeaveLvl(int lvl) {
        return Math.round(XP_BASE * Math.pow(lvl, XP_EXPO));
    }

    // gets the xp still needed to advance up one lvl
    public long getXpToNextLvl() { return progress.getXpToNextLvl(); }

    // track how full the xp bar is - a max lvl item's bar does not change
    public double getXpBarFullness() { return progress.getXpFraction(); }

    // adds the xp + returns how many lvls gained
    public int addXp(long amount) {
        int maxBefore = getItemMaxDurability();
        int xpGained = progress.addXP(amount);

        // growing max durability also grows current durability by the same amount, so leveling up doesn't leave
        // an item stuck at a lower percentage.
        if (xpGained > 0 && itemDurability > 0) {
            itemDurability = Math.min(getItemMaxDurability(), itemDurability + (getItemMaxDurability() - maxBefore));
        }
        return xpGained;
    }

    // gets the max durability of an item
    public int getItemMaxDurability() { return (int)Math.round(scale(baseMaxDurability, durabilityGrowth)); }
    public int getDurability() { return this.itemDurability; }
    public boolean isBroken() { return itemDurability <= 0; }
    public double getDurabilityFraction() { return (double) itemDurability / getItemMaxDurability(); }

    // damages the item
    public void damageItem(int amount) {
        if (amount > 0) itemDurability = Math.max(0, itemDurability - amount);
    }

    // repairs the item
    public void repairItem(int amount) {
        if (amount > 0) itemDurability = Math.min(getItemMaxDurability(), itemDurability + amount);
    }

    // scales item atk and def
    public int scaleAttack(int baseAttack) { return (int) Math.round(scale(baseAttack, attackGrowth)); }
    public int scaleDefense(int baseDefense) { return (int) Math.round(scale(baseDefense, defenseGrowth)); }

    // scales the item buff, capped at MAX_SCALE * its lvl 1 value
    // chance-type buffs are held under their cap unless the raw value starts above that cap
    public double scaleBuff(Item.ItemBuffs buff, double baseValue) {
        double value = Math.min(scale(baseValue, buffGrowth), baseValue * MAX_SCALE);
        if (CHANCE_BUFFS.contains(buff)) value = Math.min(value, Math.max(CHANCE_CEILING, baseValue));
        return value;
    }

    // gets the buff's cap, returns true if the cap is reached
    public boolean isBuffCapped(Item.ItemBuffs buff, double baseValue) {
        // cant have a value less than zero
        if (baseValue <= 0) return false;
        double value = scaleBuff(buff, baseValue);
        if (value >= baseValue * MAX_SCALE - EPSILON) return true;
        return CHANCE_BUFFS.contains(buff) && value >= Math.max(CHANCE_CEILING, baseValue) - EPSILON;
    }

    // scale the item's ability using the same rules as the buffs
    public double scaleAbility(Item.ItemAbilities ability, double baseValue) {
        double growth = abilityGrowthOverrides.getOrDefault(ability, abilityGrowth);
        return Math.min(scale(baseValue, growth), baseValue * MAX_SCALE);
    }

    private double scale(double base, double growth) {
        return base * (1.0 + growth * (getLvl() - 1));
    }
}