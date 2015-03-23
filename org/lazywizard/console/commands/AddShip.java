package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.apache.log4j.Level;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

// TODO: Once the global list of variants is retrievable through the API, this should be much simpler
public class AddShip implements BaseCommand
{
    private int tryNumber = 0;

    private FleetMemberAPI tryCreate(String variantId)
    {
        tryNumber++;
        Global.getLogger(AddShip.class).log(Level.DEBUG,
                "Try #" + tryNumber + ": " + variantId);
        try
        {
            return Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
        }
        catch (Exception ex)
        {
            return null;
        }
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

        FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI ship;
        String variant = tmp[0];

        // No variant given = spawn empty hull
        if (variant.indexOf('_') == -1)
        {
            variant += "_Hull";
        }

        // Catch common capitalization errors
        ship = tryCreate(variant);
        if (ship == null)
        {
            // Capitalize first character of variant name
            int lastUnderscore = variant.lastIndexOf('_');
            variant = variant.substring(0, lastUnderscore + 1)
                    + Character.toUpperCase(variant.charAt(lastUnderscore + 1))
                    + variant.substring(lastUnderscore + 2);
            ship = tryCreate(variant);

            if (ship == null)
            {
                // Capitalize the ship itself
                variant = Character.toUpperCase(variant.charAt(0))
                        + variant.substring(1);
                ship = tryCreate(variant);

                if (ship == null)
                {
                    // Try adding _Hull to the end
                    variant = tmp[0] + "_Hull";
                    ship = tryCreate(variant);

                    if (ship == null)
                    {
                        try
                        {
                            // Before we give up, maybe the .variant file doesn't match the ID?
                            variant = Global.getSettings().loadJSON("data/variants/"
                                    + tmp[0] + ".variant").getString("variantId");
                            ship = tryCreate(variant);
                            Console.showMessage("Warning: variant ID doesn't match"
                                    + " .variant filename!", Level.WARN);
                        }
                        catch (Exception ex)
                        {
                            Console.showMessage("No ship found with id '" + tmp[0] + "'!");
                            return CommandResult.ERROR;
                        }
                    }
                }
            }
        }

        if (ship.isFighterWing())
        {
            Console.showMessage("Use AddWing for fighters!");
            return CommandResult.ERROR;
        }

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
