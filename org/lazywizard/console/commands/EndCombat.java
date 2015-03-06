package org.lazywizard.console.commands;

import java.util.Arrays;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

public class EndCombat implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Game crashes if endCombat() is used in simulator, so snark instead
        if (context == CommandContext.COMBAT_SIMULATION)
        {
            Console.showMessage("Oh no, you're trapped! Wait, you can just hit"
                    + " escape and leave. Duh.");
            return CommandResult.WRONG_CONTEXT;
        }

        FleetSide victor;
        if (args.isEmpty())
        {
            victor = FleetSide.PLAYER;
        }
        else
        {
            try
            {
                victor = FleetSide.valueOf(args.toUpperCase());
            }
            catch (IllegalArgumentException ex)
            {
                Console.showMessage("No such side '" + args + "'! Valid sides are "
                        + CollectionUtils.implode(Arrays.asList(FleetSide.values())) + ".");
                return CommandResult.ERROR;
            }
        }

        Global.getCombatEngine().endCombat(0f, victor);
        return CommandResult.SUCCESS;
    }
}
