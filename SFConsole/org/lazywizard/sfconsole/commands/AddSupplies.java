package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import data.scripts.console.BaseCommand;
import data.scripts.console.Console;

public class AddSupplies extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of supplies to your fleet's cargo.";
    }

    @Override
    protected String getSyntax()
    {
        return "addsupplies <amount>";
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
            Console.showMessage("Error: supply amount must be a whole number!");
            return false;
        }

        Global.getSector().getPlayerFleet().getCargo().addSupplies(amount);
        Console.showMessage("Added " + amount + " supplies to player inventory.");
        return true;
    }
}
