package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import data.scripts.console.BaseCommand;
import data.scripts.console.Console;

public class AddAptitudePoints extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of aptitude points to your character.";
    }

    @Override
    protected String getSyntax()
    {
        return "addaptitudepoints <amount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        MutableCharacterStatsAPI character =
                Global.getSector().getPlayerFleet().getCommanderStats();
        int amount;

        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: Aptitude points must be a whole number!");
            return false;
        }

        character.addAptitudePoints(amount);
        Console.showMessage("Added " + amount + " aptitude points to character.");
        return true;
    }
}
