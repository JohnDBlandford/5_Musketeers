package Wear;

public final class WearConfig {
    private static final int WEAPON_PTS_PER_ATK = 1;
    private static final int ARMOR_PTS_PER_HIT = 1;
    private static final int SHIELD_PTS_PER_BLCK = 1;
    private static final double WARN_FRAC = 0.20;

    private WearConfig() {}

    public static int getWeaponPtsPerAtk() { return WEAPON_PTS_PER_ATK; }
    public static int getArmorPtsPerHit() { return ARMOR_PTS_PER_HIT; }
    public static int getShieldPtsPerBlck() { return SHIELD_PTS_PER_BLCK; }
    public static double getWarnFrac() { return WARN_FRAC; }
}
