package data.scripts.console.commands;

import data.scripts.console.BaseCombatHook;
import data.scripts.console.BaseCommand;

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
    protected boolean isCombatOnly()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        BaseCombatHook.noCooldown = !BaseCombatHook.noCooldown;
        return true;
    }
}
