package org.lazywizard.sfconsole.commands;

import org.lazywizard.sfconsole.BaseCombatHook;
import org.lazywizard.sfconsole.BaseCommand;

public class NoCooldown extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Toggles weapon cooldowns for all ships on your side.";
    }

    @Override
    protected String getSyntax()
    {
        return "nocooldown (no arguments)";
    }

    @Override
    protected boolean isUseableInCombat()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        BaseCombatHook.toggleNoCooldown();
        return true;
    }
}
