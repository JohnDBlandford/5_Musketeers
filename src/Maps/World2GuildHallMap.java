package Maps;

// Same tavern interior as World 1, but the front door leads back out to World 2.
public class World2GuildHallMap extends GuildHallMap {

    @Override
    protected String exitMap() {
        return "world2_map";
    }

    @Override
    protected float exitX() {
        return 64 * 48 + 24;
    }

    @Override
    protected float exitY() {
        return 37 * 48;
    }
}
