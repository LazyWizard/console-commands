package org.lazywizard.console.listeners;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

public class ConsoleCombatListener implements EveryFrameCombatPlugin
{
    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        // TODO: Check for input
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
    }
}
