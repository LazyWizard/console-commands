package data.scripts.console.commands;

import data.scripts.console.BaseCombatHook;
import data.scripts.console.BaseCommand;

public class God extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Toggles invincibility for all ships on your side.";
    }

    @Override
    protected String getSyntax()
    {
        return "god (no arguments)";
    }

    @Override
    protected boolean isUseableInCombat()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        BaseCombatHook.toggleGodMode();
        return true;
    }
}