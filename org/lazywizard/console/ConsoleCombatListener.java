package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import static org.lazywizard.console.CommandContext.*;

public class ConsoleCombatListener implements EveryFrameCombatPlugin
{
    private CombatEngineAPI engine;

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        // Temp fix for .6.2a bug
        if (engine != Global.getCombatEngine())
        {
            return;
        }

        Console.advance(engine.isInCampaign() ? COMBAT_CAMPAIGN : COMBAT_MISSION);
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        this.engine = engine;
    }
}
