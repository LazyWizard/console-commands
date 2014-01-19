package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import org.lazywizard.sfconsole.BaseCommand;
import org.lazywizard.sfconsole.Console;

public class AddCredits extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of credits to your fleet's account.";
    }

    @Override
    protected String getSyntax()
    {
        return "addcredits <amount>";
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
            Console.showMessage("Error: credit amount must be a whole number!");
            return false;
        }

        Global.getSector().getPlayerFleet().getCargo().getCredits().add(amount);
        Console.showMessage("Added " + amount + " credits to player inventory.");
        return true;
    }
}
