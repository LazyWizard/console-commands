package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddFuel implements BaseCommand
{
    public static void addNeededFuel(CampaignFleetAPI fleet)
    {
        float capacity = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy())
        {
            if (!member.isMothballed())
            {
                capacity += member.getFuelCapacity();
            }
        }

        fleet.getCargo().addFuel(capacity - fleet.getCargo().getFuel());
    }

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
            addNeededFuel(Global.getSector().getPlayerFleet());
            Console.showMessage("Topped up the player's fuel tanks.");
            return CommandResult.SUCCESS;
        }

        int amount;
        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: fuel amount must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        Global.getSector().getPlayerFleet().getCargo().addFuel(amount);
        Console.showMessage("Added " + amount + " fuel to player inventory.");
        return CommandResult.SUCCESS;
    }
}
