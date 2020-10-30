package org.lazywizard.console.commands;

import java.util.List;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddShips implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        String[] tmp = args.split(" ");

        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        if (tmp.length == 1)
        {
            return runCommand(args + " 1", context);
        }

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        String pattern = tmp[0].replace("*", ".+");
        String quantity = tmp[1];
        List<String> allShips = List_.getShipIds();
        List<String> ships = CommandUtils.findMatchingStrings(pattern, allShips);

        for (String ship : ships)
        {
            (new AddShip()).runCommand(ship + "_Hull " + quantity, context);
        }

        return CommandResult.SUCCESS;
    }
}
