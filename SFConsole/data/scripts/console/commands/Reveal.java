package data.scripts.console.commands;

import data.scripts.console.BaseCombatHook;
import data.scripts.console.BaseCommand;

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
    protected boolean isCombatOnly()
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
