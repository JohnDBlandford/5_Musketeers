package Item;

import GameObject.SpriteSheet;
import inventory.Inventory;

import java.util.*;

// NOTE: This class is never meant to be instantiated. Only instantiate the specific class needed
public abstract class Item {
    protected final String itemName;
    protected SpriteSheet itemLook;
    protected ItemRarity itemRarity;
    protected Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities;
    protected Map<ItemBuffs, Double> buffModifiers;
    protected final ItemStats itemStats;

    public enum ItemRarity {
        COMMON, UNCOMMON, EPIC, LEGENDARY
    }

    public enum ItemBuffs {
        CRIT_CHANCE, ATK_BOOST, CRIT_DMG, ACCURACY, PIERCING,
        BONUS_DEF, DEF_BOOST, THORNS, BONUS_MAX_HP,
        SPD_BOOST, DMG_REDUCTION, ATK_DMG, NONE
    }

    public enum ItemAbilities {
        NONE, GALE_SLASH, HEAVY_CLEAVE, BLOODFANG_STRIKE,
        RECKONING_BLOW, WINDBORNE_ARROW, PIERCING_SHOT, VOLLEY,
        HUNTERS_MARK, BULWARK_STANCE, SHIELD_BASH, SHIELD_THROW,
        RETALIATE, SECOND_WIND, LIGHT_FOOTING, HARDEN, GRIT, IRON_STANCE
    }

    public enum AbilityType {
        BUFF, BURST, SUSTAIN, FINISHER, MULTI_HIT, DEBUFF, CONTROL,
        COUNTER, HEAL, DMG_REDUCTION, SPD_BOOST, BONUS_DEF, NONE,
        HEALTH_RESTORE
    }

    // this constructor is for items that dont set its own stats so they get whatever the defaults are
    // you should almost never need to do this
    protected Item(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
                   Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
                   Map<ItemBuffs, Double> buffModifiers) {
        this(itemName, itemLook, itemRarity, itemAbilities, buffModifiers, ItemStats.getDefaultsForItemRarity(itemRarity));
    }

    // protected constructor to prevent instantiation outside of this package
    // but also allows subclasses to see it
    protected Item(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
                   Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
                   Map<ItemBuffs, Double> buffModifiers, ItemStats itemStats) {
        // common items never have buffs
        // this check is here so that this fails at compile time and not runtime
        if ((itemRarity == ItemRarity.COMMON) && (!buffModifiers.isEmpty())) {
            throw new IllegalArgumentException(itemRarity + " items cannot have buffs, but got: " + buffModifiers.keySet());
        }
        this.itemName = itemName;
        this.itemLook = itemLook;
        this.itemRarity = itemRarity;
        this.itemAbilities = itemAbilities;
        this.buffModifiers = buffModifiers;
        this.itemStats = itemStats;
    }

    protected abstract SpriteSheet assignSprite();

    public String getItemName() {
        return this.itemName;
    }

    public SpriteSheet getItemLook() {
        if (this.itemLook == null) this.itemLook = assignSprite();
        return this.itemLook;
    }

    public ItemRarity getItemRarity() {
        return this.itemRarity;
    }

    public double getItemBuffModifiers(ItemBuffs buff) {
        return buffModifiers.getOrDefault(buff, 0.0);
    }

    // new subtypes must declare a type for this method because Item is abstract
    public abstract Inventory.EquipSlot getEquipSlot();

    // note that these two methods return the entire map every time they are called
    // they are unmodifiable
    public Map<AbilityType, Map<ItemAbilities, Double>> getTotalItemAbilities() {
        return Collections.unmodifiableMap(this.itemAbilities);
    }

    public Map<ItemBuffs, Double> getTotalItemBuffs() {
        return Collections.unmodifiableMap(this.buffModifiers);
    }

    // ask nick about this but this is whats here for now because i believe we may want more of them to be flat buffs
    private static final Set<ItemBuffs> FLAT_ITEM_BUFFS = Collections.unmodifiableSet(
            EnumSet.of(ItemBuffs.BONUS_MAX_HP, ItemBuffs.BONUS_DEF, ItemBuffs.THORNS)
    );

    // returns true if the buff is a part of the FLAT_ITEM_BUFF set
    public static boolean isFlatBuff(ItemBuffs buff) { return FLAT_ITEM_BUFFS.contains(buff); }

    // these are per item stat specific methods
    public ItemStats getItemStats() { return this.itemStats; }

    // base stat methods for those items that dont set their own stats
    // all default to zero
    public int getBaseAttack() { return 0; }
    public int getBaseDefense() { return 0; }

    // these return the current lvl of the item atk and def
    public int getAttack() { return itemStats.scaleAttack(getBaseAttack()); }
    public int getDefense() { return itemStats.scaleDefense(getBaseDefense()); }

    // gets the buff at the current lvl capped at 0.95. a fresh enum map is made every call so order is stable
    public Map<ItemBuffs, Double> getScaledBuffs() {
        Map<ItemBuffs, Double> scaledBuff = new EnumMap<>(ItemBuffs.class);
        // iterate through the map to get keys and vals
        for (Map.Entry<ItemBuffs, Double> entry : buffModifiers.entrySet()) scaledBuff.put(entry.getKey(), itemStats.scaleBuff(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(scaledBuff);
    }

    // returns true once buff is maxed out and stops growing with the item lvl
    public boolean isBuffCapped(ItemBuffs buff) {
        return itemStats.isBuffCapped(buff, getItemBuffModifiers(buff));
    }

    // gets the ability at the current lvl. these are not capped. a fresh enum map is made every call
    public Map<AbilityType, Map<ItemAbilities, Double>> getScaledAbilities() {
        Map<AbilityType, Map<ItemAbilities, Double>> scaledAbility = new EnumMap<>(AbilityType.class);
        for (Map.Entry<AbilityType, Map<ItemAbilities, Double>> type : itemAbilities.entrySet()) {
            Map<ItemAbilities, Double> innerItemAbility = new EnumMap<>(ItemAbilities.class);
            for (Map.Entry<ItemAbilities, Double> ability : type.getValue().entrySet())
                innerItemAbility.put(ability.getKey(), itemStats.scaleAbility(ability.getKey(), ability.getValue()));
            scaledAbility.put(type.getKey(), Collections.unmodifiableMap(innerItemAbility));
        }
        return Collections.unmodifiableMap(scaledAbility);
    }

    // gets the current spd and bonus hp of an item at the current lvl
    public double getItemSpeed() { return scaledBuffOrZero(ItemBuffs.SPD_BOOST); }
    public double getItemBonusMaxHp() { return scaledBuffOrZero(ItemBuffs.BONUS_MAX_HP); }

    private double scaledBuffOrZero(ItemBuffs buff) {
        return buffModifiers.containsKey(buff) ? itemStats.scaleBuff(buff, buffModifiers.get(buff)) : 0.0;
    }

    // fix later
    @Override
    public String toString() {
        List<String> abilityNames = new ArrayList<>();
        for (AbilityType ability : itemAbilities.keySet()) {
            if (ability != AbilityType.NONE) abilityNames.add(ability.name());
        }
        String abilString = abilityNames.isEmpty() ? "None" : String.join(", ", abilityNames);
        return "Item: " + itemName + " [" + itemRarity + "]\nAbilities: " + abilString;
    }
}
