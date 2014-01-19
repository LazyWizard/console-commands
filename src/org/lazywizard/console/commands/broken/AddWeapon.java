package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import org.lazywizard.sfconsole.BaseCommand;
import org.lazywizard.sfconsole.Console;

public class AddWeapon extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds a weapon to your fleet's cargo.\nIf an amount"
                + " is specified, a stack of that size will be given.\n"
                + "Supports reversed arguments.";
    }

    @Override
    protected String getSyntax()
    {
        return "addweapon <weaponID> <optionalAmount>";
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

        Global.getSector().getPlayerFleet().getCargo().addItems(
                CargoItemType.WEAPONS, tmp[0], amt);

        Console.showMessage("Added " + amt + " of weapon " + tmp[0] + " to player inventory.");
        return true;
    }
}
