package data.scripts.console.commands;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.console.BaseCommand;

public class AddCommandPoints extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of command points to your fleet. You"
                + " can remove the bonus with the argument 'remove'.";
    }

    @Override
    protected String getSyntax()
    {
        return "addcommandpoints <amount>|remove";
    }

    @Override
    protected boolean isUseableInCombat()
    {
        return true;
    }

    @Override
    protected boolean runCommand(String args)
    {
        if ("remove".equals(args))
        {
            MutableStat commandPoints = getCombatEngine().getFleetManager(
                    FleetSide.PLAYER).getCommandPointsStat();
            commandPoints.unmodify("Console");
            showMessage("Removed command point bonus.");
            return true;
        }

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
        showMessage("Added " + amount + " command points.");
        return true;
    }
}
