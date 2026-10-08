package NPCs;

import Utils.Point;

public class AshWolfDummy extends TestDummy {
    public AshWolfDummy(int id, Point location) {
        // HP: 110, ATK: 30, DEF: 9, SPD: 30
        super(id, location, "Ash Wolf", 110f, 30f, 9f, 30f);
    }
}