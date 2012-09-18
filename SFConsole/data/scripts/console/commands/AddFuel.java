package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.console.BaseCommand;

public class AddFuel extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "";
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
            showMessage("Error: fuel amount must be a whole number!");
            return false;
        }

        Global.getSector().getPlayerFleet().getCargo().addFuel(amount);
        showMessage("Added " + amount + " fuel to player inventory.");
        return true;
    }
}
