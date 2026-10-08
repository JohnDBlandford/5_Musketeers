package NPCs;

import Utils.Point;

public class HollowChorusZealotDummy extends TestDummy {
    public HollowChorusZealotDummy(int id, Point location) {
        // HP: 220, ATK: 38, DEF: 24, SPD: 26
        super(id, location, "Hollow Chorus Zealot", 220f, 38f, 24f, 26f);
    }

    @Override
    protected void onHitPassive(float damageDealt) {
        if (currentHp > 0) {
            float healAmount = baseAtk * 0.15f; // 15% ATK heal-on-hit passive
            currentHp = Math.min(maxHp, currentHp + healAmount);
            System.out.printf("   └─ [PASSIVE TRIGGER] Zealot healed %.2f HP -> Current HP: %.2f / %.2f%n",
                    healAmount, currentHp, maxHp);
        }
    }
}