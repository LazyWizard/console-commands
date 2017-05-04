package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddHullmod implements BaseCommand
{
    private static final Logger Log = Logger.getLogger(AddHullmod.class);

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        final String id = CommandUtils.findBestStringMatch(args,
                Global.getSector().getCampaignUI().getAvailableHullModIds());
        if (id == null)
        {
            Console.showMessage("No modspec found with id '" + args + "'!");
            return CommandResult.ERROR;
        }

        Global.getSector().getPlayerFleet().getCargo().addHullmods(id, 1);
        Console.showMessage("Added modspec " + id + " to player inventory.");
        return CommandResult.SUCCESS;
    }
}
