package org.lazywizard.sfconsole.commands;

import org.lazywizard.sfconsole.BaseCombatHook;
import org.lazywizard.sfconsole.BaseCommand;

public class Reveal extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Toggles fog of war on the battle map.";
    }

    @Override
    protected String getSyntax()
    {
        return "reveal (no arguments)";
    }

    @Override
    protected boolean isUseableInCombat()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        BaseCombatHook.toggleReveal();
        return true;
    }
}
