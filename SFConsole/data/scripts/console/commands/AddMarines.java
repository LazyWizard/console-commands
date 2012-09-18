package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.console.BaseCommand;

public class AddMarines extends BaseCommand
{
    @Override
    protected String getName()
    {
        return "AddMarines";
    }

    @Override
    protected String getHelp()
    {
        return "";
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

        showMessage("Added " + amt + " marines to player fleet.");
        return true;
    }
}
