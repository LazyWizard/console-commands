package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FogOfWarAPI;
import com.fs.starfarer.api.plugins.BattleObjectivesEffectsPlugin;
import com.fs.starfarer.api.plugins.FogOfWarPlugin;

/**
 * Notifies the {@link ConsoleManager} when the game is in battle.
 */
public class BaseCombatHook implements BattleObjectivesEffectsPlugin, FogOfWarPlugin
{
    public static boolean shouldReveal = false;

    @Override
    public void applyEffects()
    {
        if (Console.getConsole() != null)
        {
            Console.getConsole().checkQueue();
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
        if (Console.getConsole() == null)
        {
            Console.setConsole(new Console());
        }

        shouldReveal = false;
        Console.setInBattle(true);
        Console.setCombatEngine(engine);
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

    @Override
    public void reveal(FogOfWarAPI fogOfWar)
    {
        if (shouldReveal)
        {
            fogOfWar.revealAroundPoint(this, 0, 0, 50000f);
        }
    }

    @Override
    public void hide(FogOfWarAPI fogOfWar)
    {
    }
}