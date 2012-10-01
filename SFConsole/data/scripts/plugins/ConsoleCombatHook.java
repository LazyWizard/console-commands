package data.scripts.plugins;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.plugins.CombatEnginePlugin;
import data.scripts.console.ConsoleManager;

public class ConsoleCombatHook implements CombatEnginePlugin
{
    @Override
    public void init(CombatEngineAPI engine)
    {
        ConsoleManager.setInBattle(true);
        ConsoleManager.setCombatEngine(engine);
    }
}
