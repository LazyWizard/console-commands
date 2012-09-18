package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import data.scripts.console.BaseCommand;

public class AddWeapon extends BaseCommand
{
    @Override
    protected String getName()
    {
        return "AddWeapon";
    }

    @Override
    protected String getHelp()
    {
        return "";
    }

    @Override
    protected String getSyntax()
    {
        return "addweapon <weaponID> <optionalAmount>";
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
                CargoItemType.WEAPONS, tmp[0], amt);

        showMessage("Added " + amt + " of weapon " + tmp[0] + " to player inventory.");
        return true;
    }
}
