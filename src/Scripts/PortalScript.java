package Scripts;

import java.util.ArrayList;

import GameObject.Portal;
import Level.Script;
import ScriptActions.*;

// A script that handles the interaction with a portal, allowing the player to choose whether to travel to the next area or not. This one took a lot of copilot help.

public class PortalScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        Portal portal = (Portal) getEntity();
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        actions.add(new TextboxScriptAction() {
            {
                addText("Travel to the next area?", new String[] { "Yes", "No" });
            }
        });
        actions.add(new ConditionalScriptAction() {
            {
                addConditionalScriptActionGroup(new ConditionalScriptActionGroup() {
                    {
                        addRequirement(new CustomRequirement() {
                            @Override
                            public boolean isRequirementMet() {
                                Integer selectedOption = getScriptActionOutputManager()
                                        .getFlagData("TEXTBOX_OPTION_SELECTION");
                                return selectedOption != null && selectedOption == 0;
                            }
                        });
                        addScriptAction(new ChangeMapScriptAction(
                                portal.getTargetMap(), portal.getTargetX(), portal.getTargetY()));
                    }
                });
                addConditionalScriptActionGroup(new ConditionalScriptActionGroup() {
                    {
                        addScriptAction(new DoNothingScriptAction());
                    }
                });
            }
        });
        actions.add(new UnlockPlayerScriptAction());
        return actions;
    }
}
