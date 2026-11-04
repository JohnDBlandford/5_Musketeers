package ScriptActions;

import Chest.ChestId;
import Chest.ChestOpener;
import Chest.ChestResult;

import java.util.List;
import java.util.Objects;

// Opens one chest and shows what was found. The screen is found among the map's listeners (anything that implements
// ChestOpener, i.e. PlayLevelScreen), so GameListener does not need a new method. It is a TextboxScriptAction whose text is
// only known when the script runs: setup() asks the opener, adds the result's pages to the textbox, and the inherited
// setup/execute/cleanup then show them and wait until the player has read them. If there is no opener it warns and shows
// nothing, so the script's unlock still frees the player.
public class OpenChestScriptAction extends TextboxScriptAction {
    private final ChestId chestId;

    public OpenChestScriptAction(ChestId id) {
        super();
        this.chestId = Objects.requireNonNull(id, "id");
    }

    public ChestId getChestId() {
        return chestId;
    }

    @Override
    public void setup() {
        ChestResult result = notifyOpener(map.getListeners(), chestId);
        if (result == null) {
            System.err.println("OpenChestScriptAction: no listener implements ChestOpener (or it gave no result), so chest "
                    + chestId + " was not opened.");
            return; // nothing was added, so execute() finishes straight away
        }
        for (String page : result.pages()) addText(page);
        super.setup();
    }

    // asks the FIRST ChestOpener in the list to open this chest and returns its result, or null if there is none. only one
    // opener is asked because opening one chest twice would pay twice; anything else in the list is skipped
    public static ChestResult notifyOpener(List<?> listeners, ChestId id) {
        Objects.requireNonNull(listeners, "listeners");
        Objects.requireNonNull(id, "id");
        // a listener can technically be any class because of the use of the ? operator so the type here is Object to satisfy that requirement
        for (Object listener : listeners) {
            if (listener instanceof ChestOpener opener) return opener.onOpenChest(id);
        }
        return null;
    }
}
