package BattleSystem;

import Item.Armor.Armor;
import Item.Item;
import Item.RangedWeapon.RangedWeapon;
import Item.Shield.Shield;
import Item.Sword.Sword;
import Level.Player;
import inventory.EquipSlot;
import inventory.Inventory;

import java.util.Map;
import java.util.Random;

// All the battle formulas in one place so they are easy to tune.
//
// What comes straight from your existing Java:
//   - damage = rawAttack * (1 - DEF / (DEF + 50)), minimum 1      (CombatResolver.calculateDamage, called directly)
//   - Player / Enemy stats, modifiers, XP, leveling                 (Player.java, Enemy.java, used as they are)
//   - item numbers: weapon damage, armor baseDef, shield blockChance / blockAmount, and every ItemBuffs value
// What the items do not define on their own (the few judgment calls, all in this file):
//   - WEAPON_WEIGHT: how much of a weapon's printed damage is added to your ATK
//   - the 5% base crit chance / 1.5x base crit multiplier, the dodge chance, the turn order (SPD + 1..10), and the run formula
public final class BattleMath {
    private BattleMath() {}

    // Weapons print big damage numbers (a Rusted Short Sword is 30, a Wraithbone Saber is 80) while the knight's ATK starts at 15.
    // Adding the full number would make weapons the only thing that matters, so only this fraction of it is added.
    public static final double WEAPON_WEIGHT = 0.25;

    // total of one buff over every equipped item (ATK_BOOST, CRIT_CHANCE, ...)
    public static double sumBuff(Inventory inv, Item.ItemBuffs buff) {
        double total = 0;
        for (EquipSlot slot : EquipSlot.values()) {
            Item item = inv.getEquippedItem(slot);
            if (item != null) total += item.getItemBuffModifiers(buff);
        }
        return total;
    }

    // the number an equipped item gives one of its abilities (armor passives like HARDEN 0.12 live here); 0 if nothing has it
    public static double abilityValue(Inventory inv, Item.ItemAbilities ability) {
        double total = 0;
        for (EquipSlot slot : EquipSlot.values()) {
            Item item = inv.getEquippedItem(slot);
            if (item == null) continue;
            for (Map<Item.ItemAbilities, Double> group : item.getTotalItemAbilities().values()) {
                Double v = group.get(ability);
                if (v != null) total += v;
            }
        }
        return total;
    }

    // ATK = (your ATK + a fraction of the weapon's damage) * (1 + ATK_BOOST). The weapon is the one the move uses (null = bare hands).
    public static float effectiveAtk(Player p, Inventory inv, Item weapon) {
        return (p.getBaseATK() + (float) (weaponBase(weapon) * WEAPON_WEIGHT)) * (float) (1 + sumBuff(inv, Item.ItemBuffs.ATK_BOOST));
    }

    // DEF = (your DEF + armor baseDef) * (1 + BONUS_DEF + DEF_BOOST + IRON_STANCE).
    // BONUS_DEF is stored as a fraction (0.22 on the Wraithbone shield) so it is treated as a percentage like DEF_BOOST.
    public static float effectiveDef(Player p, Inventory inv) {
        Item armor = inv.getEquippedItem(EquipSlot.ARMOR);
        float flat = p.getBaseDEF() + (armor instanceof Armor ? ((Armor) armor).getBaseDef() : 0);
        double pct = sumBuff(inv, Item.ItemBuffs.BONUS_DEF) + sumBuff(inv, Item.ItemBuffs.DEF_BOOST) + abilityValue(inv, Item.ItemAbilities.IRON_STANCE);
        return flat * (float) (1 + pct);
    }

    // SPD = your SPD * (1 + SPD_BOOST + LIGHT_FOOTING)
    public static float effectiveSpd(Player p, Inventory inv) {
        return p.getBaseSPD() * (float) (1 + sumBuff(inv, Item.ItemBuffs.SPD_BOOST) + abilityValue(inv, Item.ItemAbilities.LIGHT_FOOTING));
    }

    // share of every hit you take that is shaved off (armor DMG_REDUCTION buff + HARDEN), capped at 75%
    public static double damageReduction(Inventory inv) {
        return Math.min(0.75, sumBuff(inv, Item.ItemBuffs.DMG_REDUCTION) + abilityValue(inv, Item.ItemAbilities.HARDEN));
    }

    // share of a hit that bounces back at the attacker (the THORNS buff)
    public static double thorns(Inventory inv) {
        return sumBuff(inv, Item.ItemBuffs.THORNS);
    }

    // base damage printed on the weapon (0 if there is none)
    public static int weaponBase(Item weapon) {
        if (weapon instanceof Sword) return ((Sword) weapon).getPhysicalAtkDmg();
        if (weapon instanceof RangedWeapon) return ((RangedWeapon) weapon).getRangedDmg();
        return 0;
    }

    // the share of the target's DEF a weapon ignores (the PIERCING buff)
    public static double piercing(Item weapon) {
        return weapon == null ? 0 : Math.min(0.9, weapon.getItemBuffModifiers(Item.ItemBuffs.PIERCING));
    }

    // crit chance = 5% + weapon CRIT_CHANCE, capped at 50%; crit multiplier = 1.5 + weapon CRIT_DMG
    public static double critChance(Item weapon) {
        return Math.min(0.5, 0.05 + (weapon == null ? 0 : weapon.getItemBuffModifiers(Item.ItemBuffs.CRIT_CHANCE)));
    }

    public static double critMultiplier(Item weapon) {
        return 1.5 + (weapon == null ? 0 : weapon.getItemBuffModifiers(Item.ItemBuffs.CRIT_DMG));
    }

    // ranged accuracy = 90% + the bow's ACCURACY
    public static double rangedAccuracy(Item weapon) {
        return Math.min(1.0, 0.9 + (weapon == null ? 0 : weapon.getItemBuffModifiers(Item.ItemBuffs.ACCURACY)));
    }

    // chance a move lands = the move's own base accuracy + the weapon's ACCURACY buff (capped at 100%). Enemy dodge is rolled separately.
    public static double accuracy(Ability a, Item weapon) {
        double bonus = weapon == null ? 0 : weapon.getItemBuffModifiers(Item.ItemBuffs.ACCURACY);
        return Math.min(1.0, a.getAccuracy() + bonus);
    }

    // shield: chance to block any hit you take, and how much of that hit a block removes (the shield's own numbers)
    public static double blockChance(Inventory inv) {
        Item s = inv.getEquippedItem(EquipSlot.SHIELD);
        return s instanceof Shield ? ((Shield) s).getBlockChance() : 0;
    }

    public static double blockAmount(Inventory inv) {
        Item s = inv.getEquippedItem(EquipSlot.SHIELD);
        return s instanceof Shield ? Math.min(0.95, ((Shield) s).getBlockAmount()) : 0;
    }

    // speed-based dodge, capped at 40%
    public static float dodgeChance(float defenderSpd, float attackerSpd) {
        float c = defenderSpd / (defenderSpd + attackerSpd) * 0.5f;
        return Math.max(0f, Math.min(0.4f, c));
    }

    // turn order: SPD + 1..10
    public static float initiative(float spd, Random rng) {
        return spd + 1 + rng.nextInt(10);
    }

    // Gen 1 Pokemon run formula: F = (playerSpeed * 32) / ((enemySpeed / 4) mod 256) + 30 * attempts.
    // Escapes if F > 255, otherwise escapes if a random number 0-255 is below F.
    public static int escapeValue(int playerSpd, int enemySpd, int attempts) {
        int b = (enemySpd / 4) % 256;
        if (b == 0) return 256;
        return (playerSpd * 32) / b + 30 * attempts;
    }

    public static boolean runEscapes(int playerSpd, int enemySpd, int attempts, Random rng) {
        int f = escapeValue(playerSpd, enemySpd, attempts);
        return f > 255 || rng.nextInt(256) < f;
    }
}