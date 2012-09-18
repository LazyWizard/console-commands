package data.scripts.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import data.scripts.console.BaseCommand;

public class AdjustRelationship extends BaseCommand
{
    @Override
    protected String getName()
    {
        return "AdjustRelationship";
    }

    @Override
    protected String getHelp()
    {
        return "";
    }

    @Override
    protected String getSyntax()
    {
        return "adjustrelationship <amount>";
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
            showMessage("Error: relationship amount must be a number!");
            return false;
        }

        FactionAPI fac1 = getSector().getFaction(faction);
        FactionAPI fac2 = getSector().getFaction(towardsFaction);

        if (fac1 == null)
        {
            showMessage("Error: no such faction '" + faction + "'!");
            return false;
        }
        if (fac2 == null)
        {
            showMessage("Error: no such faction '" + towardsFaction + "'!");
            return false;
        }

        fac1.adjustRelationship(towardsFaction, newRelationship);
        showMessage("Adjusted relationship of " + faction + " towards "
                + towardsFaction + " by " + newRelationship + ".");
        return true;
    }
}
