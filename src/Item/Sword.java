package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Sword extends Item {
    private final int physicalAtkDmg;
    private ItemBuffs critChance;
    private ItemBuffs atkBoost;
    private ItemBuffs critDmg;

    Sword(String itemName, ItemBuffs buffName, Sprite itemLook,
          ItemRarity itemRarity, Map<String, Item> itemAbilities,
          Map<ItemBuffs, Item> itemBuffs, int physicalAtkDmg,
          ItemBuffs critChance, ItemBuffs atkBoost, ItemBuffs critDmg) {
        super(itemName, buffName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.physicalAtkDmg = physicalAtkDmg;
        this.critChance = critChance;
        this.atkBoost = atkBoost;
        this.critDmg = critDmg;
    }

    public int getPhysicalAtkDmg() { return this.physicalAtkDmg; }
    public ItemBuffs getCritChance() { return this.critChance; }
    public ItemBuffs getAtkBoost() { return this.atkBoost; }
    public ItemBuffs getCritDmg() { return this.critDmg; }
}
