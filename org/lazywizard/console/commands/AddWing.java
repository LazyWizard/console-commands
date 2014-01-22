package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddWing implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
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

        FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI ship;
        String variant = tmp[0];

        // Add _wing if the command fails
        try
        {
            ship = Global.getFactory().createFleetMember(
                    FleetMemberType.FIGHTER_WING, variant);
        }
        catch (Exception ex)
        {
            variant += "_wing";
            try
            {
                ship = Global.getFactory().createFleetMember(
                        FleetMemberType.FIGHTER_WING, variant);
            }
            catch (Exception ex2)
            {
                Console.showMessage("No ship found with id '" + tmp[0] + "'!");
                return CommandResult.ERROR;
            }
        }

        fleet.addFleetMember(ship);

        // More than one ship was requested
        if (amt > 1)
        {
            for (int x = 1; x < amt; x++)
            {
                ship = Global.getFactory().createFleetMember(
                        FleetMemberType.FIGHTER_WING, variant);
                fleet.addFleetMember(ship);
            }
        }

        Console.showMessage("Added " + amt + " of wing " + ship.getSpecId()
                + " to player fleet.");
        return CommandResult.SUCCESS;
    }
}
