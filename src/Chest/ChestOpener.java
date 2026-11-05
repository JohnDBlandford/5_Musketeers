package Chest;

@FunctionalInterface
public interface ChestOpener {
    ChestResult onOpenChest(ChestId id);
}
