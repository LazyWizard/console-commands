package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import org.lazywizard.sfconsole.BaseCommand;
import org.lazywizard.sfconsole.Console;

public class AddMarines extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of marines.";
    }

    @Override
    protected String getSyntax()
    {
        return "addmarines <amount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        int amt;

        try
        {
            amt = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            showSyntax();
            return false;
        }

        Global.getSector().getPlayerFleet().getCargo().addMarines(amt);

        Console.showMessage("Added " + amt + " marines to player fleet.");
        return true;
    }
}
