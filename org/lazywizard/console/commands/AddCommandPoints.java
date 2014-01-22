package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandContext;
import org.lazywizard.console.Console;
import org.lazywizard.console.Strings;

public class AddCommandPoints implements BaseCommand
{
    @Override
    public boolean runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.CAMPAIGN)
        {
            Console.showMessage(Strings.ERROR_COMBAT_ONLY);
            return false;
        }

        if ("remove".equals(args))
        {
            MutableStat commandPoints = Global.getCombatEngine().getFleetManager(
                    FleetSide.PLAYER).getCommandPointsStat();
            commandPoints.unmodify("Console");
            Console.showMessage("Removed command point bonus.");
            return true;
        }

        int amount;

        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: command point amount must be a whole number!");
            return false;
        }

        MutableStat commandPoints = Global.getCombatEngine().getFleetManager(
                FleetSide.PLAYER).getCommandPointsStat();

        commandPoints.modifyFlat("Console", amount);
        Console.showMessage("Added " + amount + " command points.");
        return true;
    }
}
