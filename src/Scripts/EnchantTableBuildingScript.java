package Scripts;

import Level.Script;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.OpenEnchantTableScriptAction;
import ScriptActions.ScriptAction;
import ScriptActions.TextboxScriptAction;

import java.util.ArrayList;
import java.util.Objects;

public class EnchantTableBuildingScript extends Script {
    public static final String[] DEFAULT_GREETING = {
            "Books and steel. They answer to each other.",
            "Bring me a tome and a blade, and I will bind them.",
            "Two of a kind make one of a higher order."
    };

    private final String[] greeting;

    public EnchantTableBuildingScript() { this(DEFAULT_GREETING); }

    public EnchantTableBuildingScript(String[] greeting) {
        Objects.requireNonNull(greeting, "array not set");
        this.greeting = greeting.clone();
    }

    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        actions.add(new TextboxScriptAction(greeting));
        actions.add(new OpenEnchantTableScriptAction());
        return actions;
    }
}
