package Item;

import GameObject.Sprite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// NOTE: This class is never meant to be instantiated. Only instantiate the specific class needed
public sealed abstract class Item permits Sword, RangedWeapon, Shield, Armor {
    protected final String itemName;
    protected Sprite itemLook;
    protected ItemRarity itemRarity;
    protected Map<ItemAbilities, AbilityType> itemAbilities;
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
        SPD_BOOST, DMG_REDUCTION, ATK_DMG, NONE
    }

    public enum ItemAbilities {
        NONE, GALE_SLASH, HEAVY_CLEAVE, BLOODFANG_STRIKE,
        RECKONING_BLOW, WINDBORNE_ARROW, PIERCING_SHOT, VOLLEY,
        HUNTERS_MARK, BULWARK_STANCE, SHIELD_BASH, SHIELD_THROW,
        RETALIATE
    }

    public enum AbilityType {
        BUFF, BURST, SUSTAIN, FINISHER, MULTI_HIT, DEBUFF, CONTROL,
        PROTECTION, COUNTER
    }

    // protected constructor to prevent instantiation outside of this package
    // but also allows subclasses to see it
    protected Item(String itemName, Sprite itemLook, ItemRarity itemRarity, Map<ItemAbilities, AbilityType> itemAbilities,
                   Map<ItemBuffs, Double> buffModifiers) {
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
    }

    public String getItemName() {
        return this.itemName;
    }

    public Sprite getItemLook() {
        return this.itemLook;
    }

    public ItemRarity getItemRarity() {
        return this.itemRarity;
    }

    // init as not equipped. can be overridden via the setter
    public EquippedStatus getItemEquippedStatus() {
        return this.equippedStatus;
    }

    public double getItemBuffModifiers(ItemBuffs buff) {
        return buffModifiers.getOrDefault(buff, 0.0);
    }

    // use for when the player picks up a new item to add to the future inventory system
    public void setEquippedStatus(EquippedStatus newEquippedStatus) {
        this.equippedStatus = newEquippedStatus;
    }

    // note that these two methods return the entire map every time they are called
    // they are unmodifiable
    public Map<ItemAbilities, AbilityType> getTotalItemAbilities() {
        return Collections.unmodifiableMap(this.itemAbilities);
    }

    public Map<ItemBuffs, Double> getTotalItemBuffs() {
        return Collections.unmodifiableMap(this.buffModifiers);
    }

    @Override
    public String toString() {
        List<String> abilityNames = new ArrayList<>();
        for (ItemAbilities ability : itemAbilities.keySet()) {
            if (ability != ItemAbilities.NONE) {
                abilityNames.add(ability.name());
            }
        }
        String abilString = abilityNames.isEmpty() ? "None" : String.join(", ", abilityNames);
        return "Item: " + itemName + " [" + itemRarity + "]\nAbilities: " + abilString;
    }
}
