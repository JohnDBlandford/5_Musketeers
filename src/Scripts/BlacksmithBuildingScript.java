package Scripts;

import java.util.ArrayList;
import Level.Script;
import ScriptActions.*;

// The interact script for a world's blacksmith: the player is locked, the smith greets them, then the blacksmith opens.
// There is deliberately NO UnlockPlayerScriptAction here: the blacksmith stays open after this script ends, and
// ShopController's onClosed callback (PlayLevelScreen passes player::unlock) frees the player when it closes.
// Attach it with  mapTile.setInteractScript(new BlacksmithBuildingScript())  (the merchant's twin is ShopBuildingScript).
public class BlacksmithBuildingScript extends Script {
    // PLACEHOLDER greeting, to be replaced with the blacksmith's real lines
    // each string is one textbox page; the engine Textbox is 30pt Arial in a 750px box, so keep a page under about 52 characters
    public static final String[] DEFAULT_GREETING = {
            "Hmph. Two of a kind? Now that I can work with.",
            "One goes into the fire. The other comes out whole.",
            "Coin first. The forge doesn't run on promises."
    };

    private final String[] greeting;

    public BlacksmithBuildingScript() {
        this(DEFAULT_GREETING);
    }

    public BlacksmithBuildingScript(String[] greeting) {
        this.greeting = greeting.clone();
    }

    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        actions.add(new TextboxScriptAction(greeting));
        actions.add(new OpenBlacksmithScriptAction());
        return actions;
    }
}
