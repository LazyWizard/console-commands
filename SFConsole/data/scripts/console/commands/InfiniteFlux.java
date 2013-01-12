package data.scripts.console.commands;

import data.scripts.console.BaseCombatHook;
import data.scripts.console.BaseCommand;

public class InfiniteFlux extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Toggles infinite flux for all ships on your side.";
    }

    @Override
    protected String getSyntax()
    {
        return "infiniteflux (no arguments)";
    }

    @Override
    protected boolean isCombatOnly()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        BaseCombatHook.toggleInfiniteFlux();
        return true;
    }
}