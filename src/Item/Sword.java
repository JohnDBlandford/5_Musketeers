package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Sword extends Item permits RustedShortSword,
        BentKitchenCleaver, ScavengersFalchion, FamineEdgeBlade,
WraithboneSaber {
    protected int physicalAtkDmg;
    protected ItemBuffs buff1;
    protected ItemBuffs buff2;
    protected ItemBuffs buff3;

    Sword(String itemName, Sprite itemLook,
          ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
          Map<ItemBuffs, Item> itemBuffs, int physicalAtkDmg,
          ItemBuffs buff1, ItemBuffs buff2, ItemBuffs buff3) {
        super(itemName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.physicalAtkDmg = physicalAtkDmg;
        this.buff1 = buff1;
        this.buff2 = buff2;
        this.buff3 = buff3;
    }

    public int getPhysicalAtkDmg() { return this.physicalAtkDmg; }
    public ItemBuffs getBuff1() { return this.buff1; }
    public ItemBuffs getBuff2() { return this.buff2; }
    public ItemBuffs getBuff3() { return this.buff3; }
}
