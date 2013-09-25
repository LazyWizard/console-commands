package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import data.scripts.console.BaseCommand;
import data.scripts.console.Console;

public class AddWing extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Tries to create a wing with the supplied variant ID and adds it"
                + " to your fleet. This command is case-sensitive.\n"
                + "If an amount is given, it will spawn that many wings of that"
                + " ID in your fleet. Ensure you have the required supplies!\n"
                + "Supports reversed arguments.";
    }

    @Override
    protected String getSyntax()
    {
        return "addwing <variantID> <optionalAmount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " 1");
        }

        if (tmp.length != 2)
        {
            showSyntax();
            return false;
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
                showSyntax();
                return false;
            }
        }

        if (amt <= 0)
        {
            return true;
        }

        FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI ship = null;
        String variant = tmp[0];

        // Add _wing if the command fails
        try
        {
            ship = Global.getFactory().createFleetMember(
                    FleetMemberType.FIGHTER_WING, variant);
        }
        catch (Exception ex)
        {
            variant = variant + "_wing";
            try
            {
                ship = Global.getFactory().createFleetMember(
                        FleetMemberType.FIGHTER_WING, variant);
            }
            catch (Exception ex2)
            {
                Console.showMessage("No ship found with id '" + tmp[0] + "'!");
                return false;
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
        return true;
    }
}
