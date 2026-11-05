package Chest;

// Identifies one chest: the map it is on and its tile. The same tile on two different maps is two different chests.
// Used as the key of ChestRegistry's opened set, so equals/hashCode by value (a record gives that for free)
public record ChestId(String mapName, int column, int row) {
    public ChestId {
        if (mapName == null || mapName.isBlank()) throw new IllegalArgumentException("Map name cannot be blank");
        if (column < 0 || row < 0) {
            throw new IllegalArgumentException("Column and row cannot be negative, but got: " + column + ", " + row);
        }
    }
}
