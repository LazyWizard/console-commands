package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.sfconsole.BaseCommand;
import org.lazywizard.sfconsole.Console;

public class SetRelationship extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Sets the relationship between two factions to the given number.";
    }

    @Override
    protected String getSyntax()
    {
        return "setrelationship <faction1> <faction2> <amount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        String[] tmp = args.split(" ");

        if (tmp.length != 3)
        {
            showSyntax();
            return false;
        }

        String faction = tmp[0];
        String towardsFaction = tmp[1];

        float newRelationship;

        try
        {
            newRelationship = Float.parseFloat(tmp[2]);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: relationship amount must be a number!");
            return false;
        }

        FactionAPI fac1 = Global.getSector().getFaction(faction);
        FactionAPI fac2 = Global.getSector().getFaction(towardsFaction);

        if (fac1 == null)
        {
            Console.showMessage("Error: no such faction '" + faction + "'!");
            return false;
        }
        if (fac2 == null)
        {
            Console.showMessage("Error: no such faction '" + towardsFaction + "'!");
            return false;
        }

        fac1.setRelationship(towardsFaction, newRelationship);
        Console.showMessage("Set relationship of " + faction + " towards "
                + towardsFaction + " to " + newRelationship + ".");
        return true;
    }
}
