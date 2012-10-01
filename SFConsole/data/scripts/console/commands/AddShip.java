package data.scripts.console.commands;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import data.scripts.console.BaseCommand;

public class AddShip extends BaseCommand
{
    @Override
    protected String getName()
    {
        return "AddShip";
    }

    @Override
    protected String getHelp()
    {
        return "Tries to create a ship with the supplied variant ID and adds it"
                + " to your fleet. This command is case-sensitive, but it will"
                + " try with different capitalization if it fails."
                + " If an amount is given, it will spawn that many ships of that"
                + " ID in your fleet. Ensure you have the required supplies!\n"
                + "A ship name with no variant attached will generate an empty"
                + " hull (the same result as 'addship <shipname>_Hull').\n"
                + "Supports reversed arguments.";
    }

    @Override
    protected String getSyntax()
    {
        return "addship <variantID> <optionalAmount>";
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
        if (variant.indexOf("_") == -1)
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
            int lastUnderscore = variant.lastIndexOf("_");
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
                    showMessage("No ship found with id '" + tmp[0] + "'!");
                    return false;
                }
            }
        }

        fleet.addFleetMember(ship);

        // More than one ship was requested
        if (amt > 1)
        {
            for (int x = 0; x < (amt - 1); x++)
            {
                ship = fact.createFleetMember(FleetMemberType.SHIP, variant);
                fleet.addFleetMember(ship);
            }
        }

        showMessage("Added " + amt + " of ship " + ship.getSpecId()
                + " to player fleet.");
        return true;
    }
}
