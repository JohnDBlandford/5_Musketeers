package Item;

import GameObject.SpriteSheet;
import inventory.EquipSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// NOTE: This class is never meant to be instantiated. Only instantiate the specific class needed
public abstract class Item {
    protected final String itemName;
    protected SpriteSheet itemLook;
    protected ItemRarity itemRarity;
    protected Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities;
    protected Map<ItemBuffs, Double> buffModifiers;

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

    // protected constructor to prevent instantiation outside of this package
    // but also allows subclasses to see it
    protected Item(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
                   Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
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
        //protected abstract SpriteSheet assignAutomaticSprite();     

    public String getItemName() {
        return this.itemName;
    }

    public SpriteSheet getItemLook() {
        return this.itemLook;
    }

    public ItemRarity getItemRarity() {
        return this.itemRarity;
    }

    public double getItemBuffModifiers(ItemBuffs buff) {
        return buffModifiers.getOrDefault(buff, 0.0);
    }

    // new subtypes must declare a type for this method because Item is abstract
    public abstract EquipSlot getEquipSlot();

    // note that these two methods return the entire map every time they are called
    // they are unmodifiable
    public Map<AbilityType, Map<ItemAbilities, Double>> getTotalItemAbilities() {
        return Collections.unmodifiableMap(this.itemAbilities);
    }

    public Map<ItemBuffs, Double> getTotalItemBuffs() {
        return Collections.unmodifiableMap(this.buffModifiers);
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
