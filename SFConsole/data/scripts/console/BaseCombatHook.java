package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.plugins.BattleObjectivesEffectsPlugin;

/**
 * Notifies the {@link ConsoleManager} when the game is in battle.
 */
public class BaseCombatHook implements BattleObjectivesEffectsPlugin
{
    @Override
    public void applyEffects()
    {
        if (Console.getManager() != null)
        {
            Console.getManager().checkQueue();
        }
    }

    /**
     * Tells {@link ConsoleManager} the game is in battle and registers the {@link CombatEngineAPI}.<p>
     *
     * Called by the game automatically at the start of each battle.
     *
     * @param engine the {@link CombatEngineAPI} the current battle uses
     */
    @Override
    public void init(CombatEngineAPI engine)
    {
        ConsoleManager.setInBattle(true);
        ConsoleManager.setCombatEngine(engine);
    }

    @Override
    public float getNavBonusPercent(int owner)
    {
        return 0f;
    }

    @Override
    public float getRangeBonusPercent(int owner)
    {
        return 0f;
    }
}