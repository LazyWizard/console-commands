package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddWing implements BaseCommand
{
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

        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " 1", context);
        }

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        /*if (!tmp[0].endsWith("_wing"))
         {
         tmp[0] = tmp[0] + "_wing";
         }*/
        int amt;

        try
        {
            amt = Integer.parseInt(tmp[1]);
        }
        catch (NumberFormatException ex)
        {
            // Support for reversed arguments
            try
            {
                amt = Integer.parseInt(tmp[0]);
                tmp[0] = tmp[1];
            }
            catch (NumberFormatException ex2)
            {
                return CommandResult.BAD_SYNTAX;
            }
        }

        if (amt <= 0)
        {
            return CommandResult.SUCCESS;
        }

        if (!tmp[0].endsWith("_wing"))
        {
            tmp[0] += "_wing";
        }

        final String variant = CommandUtils.findBestStringMatch(tmp[0],
                Global.getSector().getAllFighterWingIds());
        if (variant == null)
        {
            Console.showMessage("No ship found with id '" + tmp[0] + "'!");
            return CommandResult.ERROR;
        }

        final CargoAPI fleet = Global.getSector().getPlayerFleet().getCargo();
        fleet.addFighters(variant, amt);
        Console.showMessage("Added " + amt + " of wing LPC " + variant
                + " to player fleet.");
        return CommandResult.SUCCESS;
    }
}
