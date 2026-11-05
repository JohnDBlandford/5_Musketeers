package Story;

import Level.Player;
import NPCs.Enemy;
import inventory.Inventory;

// STORY: what the story needs from the screen that runs the game (PlayLevelScreen implements it).
public interface StoryHost {
    // a story character was talked to (TalkScript): open its conversation
    void openStoryDialogue(String storyId);

    // start a fight from the story. tag comes back to StoryDirector.onStoryBattleEnd so it can react to the result
    void startStoryBattle(Enemy enemy, boolean boss, String tag);

    Player getStoryPlayer();

    Inventory getInventory();

}