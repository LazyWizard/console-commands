package data.scripts.console.commands;

import data.scripts.console.BaseCombatHook;
import data.scripts.console.BaseCommand;

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
    protected boolean isCombatOnly()
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