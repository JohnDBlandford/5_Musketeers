package BattleSystem;

// Implemented by whoever starts a battle (PlayLevelScreen) so it hears when the battle is over.
@FunctionalInterface
public interface BattleListener {
    void onBattleEnd(Battle.BattleResult result);
}