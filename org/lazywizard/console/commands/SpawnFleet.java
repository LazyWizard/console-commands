package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SpawnFleet implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        String[] tmp = args.split(" ");

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        String faction = tmp[0];
        String fleet = tmp[1];

        try
        {
            CampaignFleetAPI toSpawn = Global.getSector().createFleet(faction, fleet);
            Global.getSector().getCurrentLocation().spawnFleet(
                    Global.getSector().getPlayerFleet(), 50, 50, toSpawn);
        }
        catch (Exception ex)
        {
            Console.showMessage("No such fleet '" + fleet + "' for faction '"
                    + faction + "'!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Fleet '" + fleet + "' successfully spawned!");
        return CommandResult.SUCCESS;
    }
}
