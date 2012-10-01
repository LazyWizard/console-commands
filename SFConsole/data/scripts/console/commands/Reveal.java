package data.scripts.console.commands;

import com.fs.starfarer.api.combat.FogOfWarAPI;
import data.scripts.console.BaseCommand;

public class Reveal extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Removes all fog of war on the battle map.";
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
        // TODO
        return true;
    }
}
