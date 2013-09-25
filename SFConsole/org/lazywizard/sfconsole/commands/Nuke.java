package org.lazywizard.sfconsole.commands;

import org.lazywizard.sfconsole.BaseCombatHook;
import org.lazywizard.sfconsole.BaseCommand;

public class Nuke extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Destroys all enemies on the battle map.";
    }

    @Override
    protected String getSyntax()
    {
        return "nuke (no arguments)";
    }

    @Override
    protected boolean isUseableInCampaign()
    {
        return false;
    }

    @Override
    protected boolean isUseableInCombat()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        BaseCombatHook.enableNuke();
        return true;
    }
}