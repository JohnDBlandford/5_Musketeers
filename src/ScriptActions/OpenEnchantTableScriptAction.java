package ScriptActions;

import Enchantment.EnchantOpener;
import Level.ScriptState;

import java.util.List;
import java.util.Objects;

// A script action that asks the screen to open the enchant table. The screen is found among the map's listeners (anything
// that implements EnchantOpener), so GameListener does not need a new method. Finishes immediately.
// Only EnchantOpeners are told: the merchant and the blacksmith have their own actions.
public class OpenEnchantTableScriptAction extends ScriptAction {
    @Override
    public ScriptState execute() {
        if (notifyOpeners(map.getListeners()) == 0) {
            System.err.println("OpenEnchantTableScriptAction: no listener implements EnchantOpener, so the enchant table was not opened.");
            // the building script never unlocks (the controller's onClosed does it), so free the player here or they stay locked forever
            if (player != null) player.unlock();
        }
        return ScriptState.COMPLETED;
    }

    // tells every EnchantOpener in the list to open the enchant table and returns how many were told; anything else is skipped
    public static int notifyOpeners(List<?> listeners) {
        Objects.requireNonNull(listeners, "listeners cannot be null");
        int told = 0;
        for (Object listener : listeners) {
            if (listener instanceof EnchantOpener opener) {
                opener.onOpenEnchantTable();
                told++;
            }
        }
        return told;
    }
}
