package data.scripts.console.commands;

import data.scripts.console.BaseCombatHook;
import data.scripts.console.BaseCommand;

public class Unreveal extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Removes the effects of the 'reveal' command.";
    }

    @Override
    protected String getSyntax()
    {
        return "unreveal (no arguments)";
    }

    @Override
    protected boolean isCombatOnly()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        BaseCombatHook.shouldReveal = false;
        return true;
    }
}
