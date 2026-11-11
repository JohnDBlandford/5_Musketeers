package Chest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Remembers two things about the chests for the whole game: where each map's chests are, and which of them are opened.
// The map object is rebuilt on every map change, so this lives on PlayLevelScreen instead (like the ShopController), which
// is what lets a rebuilt map get the same chests back and keeps an opened chest opened. A new game calls reset().
// It only stores. ChestPlacer decides where chests go and ChestController decides what an opened chest gives.
public final class ChestRegistry {
    private final Map<String, List<ChestId>> placed = new HashMap<>();
    private final Set<ChestId> opened = new HashSet<>();

    public ChestRegistry() {}

    // stores a map's chests. a map is placed once per game: a second call is an error so the placer's first answer is the one
    // that stays. an empty list is fine (no valid spots) and still counts as placed. nothing is stored if the list is rejected
    public void place(String mapName, List<ChestId> chests) {
        requireMapName(mapName);
        Objects.requireNonNull(chests, "chests not placed");
        if (placed.containsKey(mapName)) throw new IllegalStateException("Chests were already placed on " + mapName);

        Set<ChestId> seen = new HashSet<>();
        for (ChestId chest : chests) {
            if (!chest.mapName().equals(mapName)) {
                throw new IllegalArgumentException(chest + " does not belong to " + mapName);
            }
            if (!seen.add(chest)) throw new IllegalArgumentException("Chest listed twice: " + chest);
        }
        placed.put(mapName, List.copyOf(chests)); // its own unmodifiable copy, so the caller can't change it afterwards
    }

    public boolean isPlaced(String mapName) {
        requireMapName(mapName);
        return placed.containsKey(mapName);
    }

    // the chests of a map in the order they were placed. empty if the map has not been placed. opened chests stay in the list
    // because the map still has to draw them (as opened)
    public List<ChestId> getChests(String mapName) {
        requireMapName(mapName);
        return placed.getOrDefault(mapName, List.of());
    }

    public boolean isOpened(ChestId id) {
        Objects.requireNonNull(id, "id cannot be found");
        return opened.contains(id);
    }

    // true if the chest exists and this call opened it. false if it was already opened or was never placed
    public boolean markOpened(ChestId id) {
        Objects.requireNonNull(id, "id cannot be found");
        if (!getChests(id.mapName()).contains(id)) return false;
        return opened.add(id);
    }

    // a new game: nothing placed, nothing opened
    public void reset() {
        placed.clear();
        opened.clear();
    }

    private static void requireMapName(String mapName) {
        if (mapName == null || mapName.isBlank()) throw new IllegalArgumentException("Map name cannot be blank");
    }
}
