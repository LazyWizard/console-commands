package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddCommandPoints implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        if ("remove".equals(args))
        {
            MutableStat commandPoints = Global.getCombatEngine().getFleetManager(
                    FleetSide.PLAYER).getTaskManager(false).getCommandPointsStat();
            commandPoints.unmodify("Console");
            Console.showMessage("Removed command point bonus.");
            return CommandResult.SUCCESS;
        }

        if (!isInteger(args))
        {
            Console.showMessage("Error: command point amount must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final int amount = Integer.parseInt(args);
        final MutableStat commandPoints = Global.getCombatEngine().getFleetManager(
                FleetSide.PLAYER).getTaskManager(false).getCommandPointsStat();
        commandPoints.modifyFlat("Console", amount);
        Console.showMessage("Added " + format(amount) + " command points.");
        return CommandResult.SUCCESS;
    }
}
