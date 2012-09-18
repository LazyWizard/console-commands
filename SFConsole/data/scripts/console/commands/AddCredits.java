package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.console.BaseCommand;

public class AddCredits extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "";
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
            showMessage("Error: credit amount must be a whole number!");
            return false;
        }

        // Whew
        Global.getSector().getPlayerFleet().getCargo().getCredits().add(amount);
        showMessage("Added " + amount + " credits to player inventory.");
        return true;
    }
}
