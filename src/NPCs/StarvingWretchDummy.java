package NPCs;

import Utils.Point;

public class StarvingWretchDummy extends TestDummy {
    public StarvingWretchDummy(int id, Point location) {
        // HP: 70, ATK: 18, DEF: 10, SPD: 22
        super(id, location, "Starving Wretch", 70f, 18f, 10f, 22f);
    }
}