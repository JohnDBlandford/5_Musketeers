package NPCs;

import Utils.Point;

public class ReckonerBruteDummy extends TestDummy {
    public ReckonerBruteDummy(int id, Point location) {
        // HP: 150, ATK: 24, DEF: 26, SPD: 12
        super(id, location, "Reckoner Brute", 150f, 24f, 26f, 12f);
    }
}