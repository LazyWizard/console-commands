package org.lazywizard.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

// TODO: Javadoc this and add to changelog and tutorial
public abstract class BaseCombatToggleCommand implements EveryFrameCombatPlugin
{
    public abstract void onActivate(CombatEngineAPI engine);

    public abstract void advance(float amount);

    public abstract void onDeactivate(CombatEngineAPI engine);

    @Override
    public final void advance(float amount, List<InputEventAPI> events)
    {
    }

    @Override
    public final void init(CombatEngineAPI engine)
    {
    }
}
