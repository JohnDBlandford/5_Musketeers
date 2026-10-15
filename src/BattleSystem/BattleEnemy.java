package BattleSystem;

import Level.Player;

import java.util.Random;

// OPTIONAL. An enemy class (NPCs.World1.AshWolf, ...) that implements this decides its own moves in battle, so the battle uses
// ITS moves, multipliers, passives and phases instead of the generic list in EnemyMoves. Enemies that don't implement it keep
// working as before.
//
//   chooseBattleMove  this turn's move (the class's executeTurn, but returning the move instead of doing the damage itself).
//                     Build it with EnemyMoves.Move.attack(name, multiplier) / status(name) / flee(name) plus .hits(n),
//                     .lowersPlayer(stat, amt), .raisesSelf(stat, amt), .lowersSelf(stat, amt), .accuracy(a).
//   onBattleStart     called once when the fight begins (reset turn counters, re-apply passives, ...)
//   pollMessage       text to show before this turn's move (e.g. "Guard has worn off!"), or null
//   onDealtDamage     called after the move hit the knight for this much in total; return a line to show, or null
//   onBattleDefeat    called when the enemy's HP reaches 0. null = it faints. A boss with phases restores its HP, switches
//                     phase and returns the lines to show; the fight goes on.
//   battleLevel       the level to show next to its name (-1 = use the level passed to startBattle)
public interface BattleEnemy {
    EnemyMoves.Move chooseBattleMove(Random rng, Player player);

    default void onBattleStart() { }

    default String pollMessage() { return null; }

    default String onDealtDamage(int damage) { return null; }

    default String[] onBattleDefeat() { return null; }

    default int battleLevel() { return -1; }
}