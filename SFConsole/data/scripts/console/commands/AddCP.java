package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.console.BaseCommand;

public class AddCP extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of command points to your fleet.";
    }

    @Override
    protected String getSyntax()
    {
        return "addcp <amount>";
    }

    @Override
    protected boolean isCombatOnly()
    {
        return true;
    }

    @Override
    protected boolean runCommand(String args)
    {
        int amount;

        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            showMessage("Error: command point amount must be a whole number!");
            return false;
        }

        MutableStat commandPoints = getCombatEngine().getFleetManager(
                FleetSide.PLAYER).getCommandPointsStat();

        commandPoints.modifyFlat("Console", amount);
        return true;
    }
}
