package Engine;

import Level.Player;
import NPCs.Enemy;

public class CombatResolver {

    // Effective Stat Getters (using base stats & active modifiers)
    public static float getEffectiveATK(Player p) {
        return p.getBaseATK();
    }

    public static float getEffectiveATK(Enemy e) {
        return e.getBaseATK();
    }

    public static float getEffectiveDEF(Player p) {
        return p.getBaseDEF();
    }

    public static float getEffectiveDEF(Enemy e) {
        return e.getBaseDEF();
    }

    public static float getEffectiveSPD(Player p) {
        return p.getBaseSPD();
    }

    public static float getEffectiveSPD(Enemy e) {
        return e.getBaseSPD();
    }

    // Damage Calculation with Defense Mitigation
    public static float calculateDamage(float rawAttack, float defenderTotalDef) {
        float damageReduction = defenderTotalDef / (defenderTotalDef + 50.0f);
        return Math.max(1.0f, rawAttack * (1.0f - damageReduction));
    }

    // Resolves Player Attack on Enemy
    public static void resolvePlayerAttack(Player attacker, Enemy defender, float moveMultiplier) {
        float rawDamage = getEffectiveATK(attacker) * moveMultiplier;
        float finalDamage = calculateDamage(rawDamage, getEffectiveDEF(defender));
        defender.applyDamage(finalDamage, attacker);
    }

    // Resolves Enemy Attack on Player
    public static void resolveEnemyAttack(Enemy attacker, Player defender, float moveMultiplier) {
        float rawDamage = getEffectiveATK(attacker) * moveMultiplier;
        float finalDamage = calculateDamage(rawDamage, getEffectiveDEF(defender));
        defender.applyDamage(finalDamage);
    }
}