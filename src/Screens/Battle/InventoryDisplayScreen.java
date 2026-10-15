package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;
import Engine.Screen;
import inventory.EquipSlot;
import inventory.Inventory;

import java.awt.*;

// The small strip under the enemy's plate:  M S R H
// M, S, R light up when a melee weapon, shield or ranged weapon is equipped, H lights up while the heal is still available.
public class InventoryDisplayScreen extends Screen {
    private final Inventory inventory;
    private final PlayBattleScreen battle;

    public InventoryDisplayScreen(Inventory inventory, PlayBattleScreen battle) {
        this.inventory = inventory;
        this.battle = battle;
    }

    @Override
    public void initialize() {}

    @Override
    public void update() {}

    @Override
    public void draw(GraphicsHandler gh) {
        String[] letters = {"M", "S", "R", "H"};
        Color[] colors = {BattleUI.BTN_MELEE, BattleUI.BTN_SHIELD, BattleUI.BTN_RANGED, BattleUI.BTN_HEAL};
        boolean[] lit = {
                inventory.getEquippedItem(EquipSlot.MELEE) != null,
                inventory.getEquippedItem(EquipSlot.SHIELD) != null,
                inventory.getEquippedItem(EquipSlot.RANGED_WEAPON) != null,
                battle.getHealsLeft() > 0};
        for (int i = 0; i < 4; i++) {
            int x = 10 + i * 38, y = 112;
            BattleUI.button(gh, x, y, 32, 32, colors[i], false, !lit[i]);
            BattleUI.textCenter(gh, letters[i], x + 16, y + 12, 1 + 1, BattleUI.WHITE);
        }
        if (battle.getHealsLeft() > 1) BattleUI.shadowText(gh, "X" + battle.getHealsLeft(), 10 + 4 * 38, 126, 1, BattleUI.WHITE);
    }
}