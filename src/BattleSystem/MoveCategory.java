package BattleSystem;

import inventory.Inventory;

// The three kinds of moves in the options menu (Heal and Run are not move categories)
public enum MoveCategory {
    MELEE("MELEE", Inventory.EquipSlot.MELEE),
    SHIELD("SHIELD", Inventory.EquipSlot.SHIELD),
    RANGED("RANGED", Inventory.EquipSlot.RANGED_WEAPON);

    private final String label;
    private final Inventory.EquipSlot slot;

    MoveCategory(String label, Inventory.EquipSlot slot) {
        this.label = label;
        this.slot = slot;
    }

    public String getLabel() { return label; }
    public Inventory.EquipSlot getSlot() { return slot; }
}