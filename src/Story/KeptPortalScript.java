package Story;

import GameObject.Portal;
import Level.Script;
import Level.ScriptState;
import ScriptActions.*;

import java.util.ArrayList;

// STORY: the portal west of Brackenford is the Kept's standing stone. It only wakes once Old Maud has spoken (THE KEPT
// REMEMBER); before that it is cold. When it is awake it works like PortalScript (Travel? Yes / No), and stepping through
// finishes the quest. StoryDirector.LOCK_WORLD2_PORTAL = false keeps it always open (for testing World 2).
public class KeptPortalScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        Portal portal = (Portal) getEntity();
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        actions.add(new ConditionalScriptAction() {{
            // awake: the normal portal question
            addConditionalScriptActionGroup(new ConditionalScriptActionGroup() {{
                addRequirement(new CustomRequirement() {
                    @Override
                    public boolean isRequirementMet() { return StoryDirector.portalOpen(); }
                });
                addScriptAction(new TextboxScriptAction() {{
                    addText("The stone hums.\nStep into the past?", new String[] { "Yes", "No" });
                }});
                addScriptAction(new ConditionalScriptAction() {{
                    addConditionalScriptActionGroup(new ConditionalScriptActionGroup() {{
                        addRequirement(new CustomRequirement() {
                            @Override
                            public boolean isRequirementMet() {
                                Integer selected = getScriptActionOutputManager().getFlagData("TEXTBOX_OPTION_SELECTION");
                                return selected != null && selected == 0;
                            }
                        });
                        addScriptAction(new ScriptAction() {
                            @Override
                            public ScriptState execute() { StoryDirector.onPortalUsed(); return ScriptState.COMPLETED; }
                        });
                        addScriptAction(new ChangeMapScriptAction(portal.getTargetMap(), portal.getTargetX(), portal.getTargetY()));
                    }});
                    addConditionalScriptActionGroup(new ConditionalScriptActionGroup() {{
                        addScriptAction(new DoNothingScriptAction());
                    }});
                }});
            }});
            // still asleep
            addConditionalScriptActionGroup(new ConditionalScriptActionGroup() {{
                addScriptAction(new TextboxScriptAction() {{
                    addText("A standing stone, cold and carved with an open eye.");
                    addText("It does not wake for you. The Kept would know\nhow to wake it.");
                }});
            }});
        }});
        actions.add(new UnlockPlayerScriptAction());
        return actions;
    }
}