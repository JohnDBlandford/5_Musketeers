package Item;

import GameObject.Sprite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// NOTE: This class is never meant to be instantiated. Only instantiate the specific class needed
public sealed abstract class Item permits Sword, RangedWeapon, Shield, Armor {
   protected final String itemName;
    protected Sprite itemLook;
    protected ItemRarity itemRarity;
    protected Map<ItemAbilities, Item> itemAbilities;
    protected Map<ItemBuffs, Double> buffModifiers;
    protected EquippedStatus equippedStatus = EquippedStatus.NOT_EQUIPPED;

    public enum EquippedStatus {
        EQUIPPED, NOT_EQUIPPED
    }

    public enum ItemRarity {
        COMMON, UNCOMMON, EPIC, LEGENDARY
    }

    public enum ItemBuffs {
        CRIT_CHANCE, ATK_BOOST, CRIT_DMG, ACCURACY, PIERCING,
        BONUS_DEF, DEF_BOOST, THORNS, BONUS_MAX_HP,
        SPD_BOOST, DMG_REDUCTION, NONE
    }

    // placeholder for when nick gets back to me with the abilities
    public enum ItemAbilities {
        NONE
    }

    // protected constructor to prevent instantiation outside of this package
    // but also allows subclasses to see it
    protected Item(String itemName, Sprite itemLook, ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
                   Map<ItemBuffs, Double> buffModifiers) {
        // common items never have buffs
        // this check is here so that this fails at compile time and not runtime
        if ((itemRarity == ItemRarity.COMMON) && (!buffModifiers.isEmpty())) {
            throw new IllegalArgumentException(itemRarity + " items cannot have buffs, got: " + buffModifiers.keySet());
        }
        this.itemName = itemName;
        this.itemLook = itemLook;
        this.itemRarity = itemRarity;
        this.itemAbilities = itemAbilities;
        this.buffModifiers = buffModifiers;
    }

    public String getItemName() { return this.itemName; }
    public Sprite getItemLook() { return this.itemLook; }
    public ItemRarity getItemRarity() { return this.itemRarity; }
    // init as not equipped. can be overridden via the setter
    public EquippedStatus getItemEquippedStatus() {
        return this.equippedStatus;
    }

    public double getItemBuffModifiers(ItemBuffs buff) {
        return buffModifiers.getOrDefault(buff, 0.0);
    }
    // use for when the player picks up a new item to add to the future
    // inventory system
    public void setEquippedStatus(EquippedStatus newEquippedStatus) {
        this.equippedStatus = newEquippedStatus;
    }

    // note that these two methods return the entire map every time they are
    // called. i think i may have two more methods to just get an index from
    // the maps
    public Map<ItemAbilities, Item> getTotalItemAbilities() {
        return this.itemAbilities;
    }
    public Map<ItemBuffs, Double> getTotalItemBuffs() {
        return this.buffModifiers;
    }

    public void addItemAbility(Item item) {
        ItemAbilities key = ItemAbilities.valueOf(item.getItemName());
        // put instead of putIfAbsent because we do want to be able to
        // upgrade items which putIfAbsent wouldnt allow for
        itemAbilities.put(key, item);
    }

    // fix this later
    @Override
    public String toString() {
        List<String> abilityNames = new ArrayList<>();
        // loop through list to get ability names from map vals
        for (Item ability : itemAbilities.values()) {
            if (ability != null  && ability.getItemName() != null) {
                abilityNames.add(ability.getItemName());
            }
        }
        // format into a human-readable string
        String abilString = abilityNames.isEmpty() ? "None" : String.join(", ", abilityNames);
        // return formatted msg
        return "Item: " + (itemName != null ? itemName : "Unknown Item") + " [" + (itemRarity != null ? itemRarity : "COMMON") + "]\n" + "Abilities: " + abilString;
    }
}
