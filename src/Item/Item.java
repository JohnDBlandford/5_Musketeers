package Item;

import GameObject.Sprite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// NOTE: This class is never meant to be instantiated. Only instantiate the
// specific class needed
public sealed abstract class Item permits Sword, RangedWeapon, Shield,
        Armor {
   private final String itemName;
   private ItemBuffs buffName;
    private final Sprite itemLook;
    private ItemRarity itemRarity;
    // string equals the item ability description. once we decide on what
    // item abilities there will be, the String becomes a ItemAbility enum
    private Map<String, Item> itemAbilities;
    private Map<ItemBuffs, Item> itemBuffs;

    public enum EquippedStatus {
        EQUIPPED, NOT_EQUIPPED
    }

    public enum ItemRarity {
        COMMON, UNCOMMON, EPIC, LEGENDARY
    }

    public enum ItemBuffs {
        CRIT_CHANCE, ATK_BOOST, CRIT_DMG, ACCURACY, PIERCING,
        BONUS_DEF, DEF_BOOST, THORNS, BONUS_MAX_HP,
        SPD_BOOST, DMG_REDUCTION
    }

    // protected constructor to prevent instantiation outside of this package
    // but also allows subclasses to see it
    protected Item(String itemName, ItemBuffs buffName, Sprite itemLook,
                   ItemRarity itemRarity, Map<String, Item> itemAbilities,
                   Map<ItemBuffs, Item> itemBuffs) {
        this.itemName = itemName;
        this.buffName = buffName;
        this.itemLook = itemLook;
        this.itemRarity = itemRarity;
        this.itemAbilities = itemAbilities;
        this.itemBuffs = itemBuffs;
    }

    public String getItemName() { return this.itemName; }
    public ItemBuffs getBuffName() { return this.buffName; }
    public Sprite getItemLook() { return this.itemLook; }
    public ItemRarity getItemRarity() { return this.itemRarity; }
    // init as not equipped
    public EquippedStatus getItemEquippedStatus() {
        return EquippedStatus.NOT_EQUIPPED;
    }

    // note that these two methods return the entire map every time they are
    // called. i think i may have two more methods to just get an index from
    // the maps
    public Map<String, Item> getTotalItemAbilities(Item item) {
        return this.itemAbilities;
    }
    public Map<ItemBuffs, Item> getTotalItemBuffs(Item item) {
        return this.itemBuffs;
    }

    public void addItemAbility(Item item) {
        String key = item.getItemName();
        // put instead of putIfAbsent because we do want to be able to
        // upgrade items which putIfAbsent wouldnt allow for
        itemAbilities.put(key, item);
    }

    public void addItemBuffs(Item item) {
        ItemBuffs key = item.getBuffName();
        itemBuffs.put(key, item);
    }

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
        String abilString = abilityNames.isEmpty() ? "None" :
                String.join(", ", abilityNames);
        // return formatted msg
        return "Item: " + (itemName != null ? itemName : "Unknown Item")
                + " [" + (itemRarity != null ? itemRarity : "COMMON") + "]\n" +
                "Abilities: " + abilString;
    }
}
