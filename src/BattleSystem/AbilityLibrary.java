package BattleSystem;

import Item.Item;
import BattleSystem.Ability.Effect;

import java.util.List;
import java.util.Map;

// Every move in the game. Each weapon category has 4 abilities (slots 1-4 in the ability window).
// A weapon only offers the abilities it actually has (item.getTotalItemAbilities()), so a weak sword can have fewer than 4.
// NOTE: the powers, accuracies and descriptions below are PLACEHOLDERS, tune them freely.
// Format: key, name, category, POWER, ACCURACY, effect, description. Ranged moves start at 90% (the bow's ACCURACY buff is added on top); melee and shield moves start at 100%.
public final class AbilityLibrary {
    private AbilityLibrary() {}

    private static final List<Ability> MELEE = List.of(
            new Ability(Item.ItemAbilities.GALE_SLASH, "GALE SLASH", MoveCategory.MELEE, 0.9, 1.0, Effect.ATK_UP, "A quick slash. Raises your ATK for the rest of the fight."),
            new Ability(Item.ItemAbilities.HEAVY_CLEAVE, "HEAVY CLEAVE", MoveCategory.MELEE, 1.8, 1.0, Effect.BURST, "A huge overhead swing. Very high damage."),
            new Ability(Item.ItemAbilities.BLOODFANG_STRIKE, "BLOODFANG STRIKE", MoveCategory.MELEE, 1.0, 1.0, Effect.LIFESTEAL, "Heals you for half of the damage dealt."),
            new Ability(Item.ItemAbilities.RECKONING_BLOW, "RECKONING BLOW", MoveCategory.MELEE, 1.2, 1.0, Effect.FINISHER, "Deals double damage if the enemy is under 30% HP."));

    private static final List<Ability> RANGED = List.of(
            new Ability(Item.ItemAbilities.WINDBORNE_ARROW, "WINDBORNE ARROW", MoveCategory.RANGED, 0.8, 0.9, Effect.SPD_UP, "A light arrow carried on the wind. Raises your SPD."),
            new Ability(Item.ItemAbilities.PIERCING_SHOT, "PIERCING SHOT", MoveCategory.RANGED, 1.3, 0.9, Effect.PIERCE, "Ignores half of the enemy's DEF."),
            new Ability(Item.ItemAbilities.VOLLEY, "VOLLEY", MoveCategory.RANGED, 0.55, 0.9, Effect.MULTI_HIT, "Fires three arrows in a row."),
            new Ability(Item.ItemAbilities.HUNTERS_MARK, "HUNTERS MARK", MoveCategory.RANGED, 0.6, 0.9, Effect.DEF_DOWN, "Marks the enemy and lowers its DEF."));

    private static final List<Ability> SHIELD = List.of(
            new Ability(Item.ItemAbilities.BULWARK_STANCE, "BULWARK STANCE", MoveCategory.SHIELD, 0, 1.0, Effect.GUARD, "Brace behind your shield. The next hit you take is blocked."),
            new Ability(Item.ItemAbilities.SHIELD_BASH, "SHIELD BASH", MoveCategory.SHIELD, 0.5, 1.0, Effect.STUN, "A heavy bash. 40% chance to stun the enemy."),
            new Ability(Item.ItemAbilities.SHIELD_THROW, "SHIELD THROW", MoveCategory.SHIELD, 1.0, 1.0, Effect.THROW, "Hurl your shield. Better shields hit harder."),
            new Ability(Item.ItemAbilities.RETALIATE, "RETALIATE", MoveCategory.SHIELD, 0, 1.0, Effect.COUNTER, "Counter-attack the next hit you take."));

    // used when nothing is equipped in that slot, so the player is never stuck without a move
    private static final Ability PUNCH = new Ability(Item.ItemAbilities.NONE, "PUNCH", MoveCategory.MELEE, 0.6, 1.0, Effect.STRIKE, "No weapon equipped. A weak punch.");
    private static final Ability ROCK = new Ability(Item.ItemAbilities.NONE, "THROW ROCK", MoveCategory.RANGED, 0.5, 0.9, Effect.STRIKE, "No ranged weapon equipped. Chuck a rock.");
    private static final Ability BRACE = new Ability(Item.ItemAbilities.NONE, "BRACE", MoveCategory.SHIELD, 0, 1.0, Effect.GUARD, "No shield equipped. Cover up with your arms.");

    public static List<Ability> forCategory(MoveCategory category) {
        switch (category) {
            case MELEE: return MELEE;
            case RANGED: return RANGED;
            default: return SHIELD;
        }
    }

    public static Ability defaultFor(MoveCategory category) {
        switch (category) {
            case MELEE: return PUNCH;
            case RANGED: return ROCK;
            default: return BRACE;
        }
    }

    // the 4 slots for a category; a slot is null if the equipped item does not have that ability
    public static Ability[] slotsFor(MoveCategory category, Item equipped) {
        Ability[] slots = new Ability[4];
        if (equipped == null) {
            slots[0] = defaultFor(category);
            return slots;
        }
        List<Ability> all = forCategory(category);
        for (int i = 0; i < 4; i++) {
            if (itemHasAbility(equipped, all.get(i).getKey())) slots[i] = all.get(i);
        }
        return slots;
    }

    private static boolean itemHasAbility(Item item, Item.ItemAbilities key) {
        for (Map<Item.ItemAbilities, Double> group : item.getTotalItemAbilities().values()) {
            if (group.containsKey(key)) return true;
        }
        return false;
    }
}