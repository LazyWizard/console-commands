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
        return "Adds a resource to your fleet's cargo.\nIf no amount"
                + " is specified, only one of that item will be given.\n"
                + "Supports reversed arguments.";
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

        int amt;

        try
        {
            amt = Integer.parseInt(tmp[1]);
        }
        catch (NumberFormatException ex)
        {
            // Support for reversed arguments
            try
            {
                amt = Integer.parseInt(tmp[0]);
                tmp[0] = tmp[1];
            }
            catch (NumberFormatException ex2)
            {
                showSyntax();
                return false;
            }
        }

        Global.getSector().getPlayerFleet().getCargo().addItems(
                CargoItemType.RESOURCES, tmp[0], amt);

        showMessage("Added " + amt + " of item " + tmp[0] + " to player inventory.");
        return true;
    }
}
