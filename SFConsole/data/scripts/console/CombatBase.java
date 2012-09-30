package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.plugins.BattleObjectivesEffectsPlugin;

public class CombatBase implements BattleObjectivesEffectsPlugin
{
    @Override
    public void applyEffects()
    {
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
    public void init(CombatEngineAPI engine)
    {
        Console.setManager(null);

        ConsoleManager.setInBattle(true);
    }
}
