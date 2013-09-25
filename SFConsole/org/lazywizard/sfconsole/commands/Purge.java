/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lazywizard.sfconsole.BaseCommand;
import org.lazywizard.sfconsole.Console;

public class Purge extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Removes excess stacks from a station's cargo. By default it"
                + " will purge down to one stack per item, but you can also"
                + " use an optional 'max stacks' parameter with this command.";
    }

    @Override
    protected String getSyntax()
    {
        return "purge <station name> <optionalMaxStacksPerItem>";
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
        FactoryAPI fact = Global.getFactory();
        FleetMemberAPI ship;
        String variant = tmp[0];

        // No variant given = spawn empty hull
        if (variant.indexOf('_') == -1)
        {
            variant = variant + "_Hull";
        }

        // Catch common capitalization errors
        try
        {
            //showMessage("DEBUG: " + variant);
            ship = fact.createFleetMember(FleetMemberType.SHIP, variant);
        }
        catch (Exception ex)
        {
            // Capitalize first character of variant name
            int lastUnderscore = variant.lastIndexOf('_');
            variant = variant.substring(0, lastUnderscore + 1)
                    + Character.toUpperCase(variant.charAt(lastUnderscore + 1))
                    + variant.substring(lastUnderscore + 2);
            //showMessage("DEBUG: " + variant);

            try
            {
                ship = fact.createFleetMember(FleetMemberType.SHIP, variant);
            }
            catch (Exception ex2)
            {
                // One last try, capitalize the ship itself
                variant = Character.toUpperCase(variant.charAt(0))
                        + variant.substring(1);
                //showMessage("DEBUG: " + variant);

                try
                {
                    ship = fact.createFleetMember(FleetMemberType.SHIP, variant);
                }
                catch (Exception ex3)
                {
                    Console.showMessage("No ship found with id '" + tmp[0] + "'!");
                    return false;
                }
            }
        }

        fleet.addFleetMember(ship);

        // More than one ship was requested
        if (amt > 1)
        {
            for (int x = 1; x < amt; x++)
            {
                ship = fact.createFleetMember(FleetMemberType.SHIP, variant);
                fleet.addFleetMember(ship);
            }
        }

        Console.showMessage("Added " + amt + " of ship " + ship.getSpecId()
                + " to player fleet.");
        return true;
    }
}
