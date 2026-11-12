package Chest;

@FunctionalInterface
public interface ChestOpener {
    /**
     * @author Derek
     * @param id the chest id
     * @return returns whether the chest was opened successfully
     */
    ChestResult onOpenChest(ChestId id);
}
