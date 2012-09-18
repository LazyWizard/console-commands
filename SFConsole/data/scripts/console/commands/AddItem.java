package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import data.scripts.console.BaseCommand;

public class AddItem extends BaseCommand
{
    @Override
    protected String getName()
    {
        return "AddItem";
    }

    @Override
    protected String getHelp()
    {
        return "";
    }

    @Override
    protected String getSyntax()
    {
        return "additem <itemID> <optionalAmount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " 1");
        }

        if (tmp.length != 2)
        {
            showSyntax();
            return false;
        }

        float amt;

        try
        {
            amt = Float.parseFloat(tmp[1]);
        }
        catch (NumberFormatException ex)
        {
            showSyntax();
            return false;
        }

        Global.getSector().getPlayerFleet().getCargo().addItems(
                CargoItemType.RESOURCES, tmp[0], amt);

        showMessage("Added " + amt + " of item " + tmp[0] + " to player inventory.");
        return true;
    }
}
