package org.lazywizard.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;

// TODO: Javadoc this and add to changelog and tutorial
public abstract class BaseCombatTogglePlugin
{
    public abstract void onActivate(CombatEngineAPI engine);

    public abstract void advance(float amount);

    public abstract void onDeactivate(CombatEngineAPI engine);
}
