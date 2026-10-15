package BattleSystem;

import inventory.EquipSlot;

// The three kinds of moves in the options menu (Heal and Run are not move categories)
public enum MoveCategory {
    MELEE("MELEE", EquipSlot.MELEE),
    SHIELD("SHIELD", EquipSlot.SHIELD),
    RANGED("RANGED", EquipSlot.RANGED_WEAPON);

    private final String label;
    private final EquipSlot slot;

    MoveCategory(String label, EquipSlot slot) {
        this.label = label;
        this.slot = slot;
    }

    public String getLabel() { return label; }
    public EquipSlot getSlot() { return slot; }
}