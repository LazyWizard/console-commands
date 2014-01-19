package org.lazywizard.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import org.lazywizard.console.Console.Context;

public class ConsoleCombatListener implements EveryFrameCombatPlugin
{
    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        Console.checkInput(Context.COMBAT);
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
    }
}
