package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.plugins.CombatEnginePlugin;

/**
 * Notifies the {@link ConsoleManager} when the game is in battle.
 */
public abstract class BaseCombatHook implements CombatEnginePlugin
{
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
}