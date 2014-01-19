package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import org.lazywizard.sfconsole.BaseCommand;
import org.lazywizard.sfconsole.Console;

public class AddFuel extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of fuel to your fleet's cargo.";
    }

    @Override
    protected String getSyntax()
    {
        return "addfuel <amount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        int amount;

        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: fuel amount must be a whole number!");
            return false;
        }

        Global.getSector().getPlayerFleet().getCargo().addFuel(amount);
        Console.showMessage("Added " + amount + " fuel to player inventory.");
        return true;
    }
}
