package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.apache.log4j.Level;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddShip implements BaseCommand
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

        // TODO: Test this once 0.7a lands
        String variant = null;

        // Test for variants
        final String toLower = tmp[0].toLowerCase();
        for (String id : Global.getSettings().getAllVariantIds())
        {
            if (toLower.equals(id.toLowerCase()))
            {
                variant = id;
                break;
            }
        }

        // Test for empty hulls
        if (variant == null)
        {
            final String withHull = toLower + "_hull";
            for (String id : Global.getSettings().getAllVariantIds())
            {
                if (withHull.equals(id.toLowerCase()))
                {
                    variant = id;
                    break;
                }
            }
        }

        // Before we give up, maybe the .variant file doesn't match the ID?
        if (variant == null)
        {
            try
            {
                variant = Global.getSettings().loadJSON("data/variants/"
                        + tmp[0] + ".variant").getString("variantId");
                Console.showMessage("Warning: variant ID doesn't match"
                        + " .variant filename!", Level.WARN);
            }
            catch (Exception ex)
            {
                Console.showMessage("No ship found with id '" + tmp[0] + "'!");
                return CommandResult.ERROR;
            }
        }

        // We've finally verified the variant id, now create the actual ship
        FleetMemberAPI ship;
        try
        {
            ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        }
        catch (Exception ex)
        {
            Console.showException("Failed to create variant '" + variant + "'!", ex);
            return CommandResult.ERROR;
        }

        // TODO: Redirect wings to AddWing instead of snarking at the user
        if (ship.isFighterWing())
        {
            Console.showMessage("Use AddWing for fighters!");
            return CommandResult.ERROR;
        }

        final FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        fleet.addFleetMember(ship);

        // More than one ship was requested
        if (amt > 1)
        {
            for (int x = 1; x < amt; x++)
            {
                ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
                fleet.addFleetMember(ship);
            }
        }

        Console.showMessage("Added " + amt + " of ship " + ship.getSpecId()
                + " to player fleet.");
        return CommandResult.SUCCESS;
    }
}
