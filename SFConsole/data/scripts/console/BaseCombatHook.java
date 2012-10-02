package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.plugins.CombatEnginePlugin;

public abstract class BaseCombatHook implements CombatEnginePlugin
{
    @Override
    public void init(CombatEngineAPI engine)
    {
        ConsoleManager.setInBattle(true);
        ConsoleManager.setCombatEngine(engine);
    }
}