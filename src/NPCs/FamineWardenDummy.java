package NPCs;

import Utils.Point;

public class FamineWardenDummy extends TestDummy {
    public FamineWardenDummy(int id, Point location) {
        // Mini-Boss: HP: 380, ATK: 48, DEF: 32, SPD: 28
        super(id, location, "The Famine Warden (Mini-Boss)", 380f, 48f, 32f, 28f);
    }
}