package BattleSystem;

// Implemented by whoever starts a battle (PlayLevelScreen) so it hears when the battle is over.
public interface BattleListener {
    void onBattleEnd(BattleResult result);
}