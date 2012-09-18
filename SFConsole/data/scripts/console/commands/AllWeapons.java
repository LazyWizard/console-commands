package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import data.scripts.console.BaseCommand;
import java.util.List;

public class AllWeapons extends BaseCommand
{
    private static final String STATION = "Abandoned Storage Facility";
    private static final float STACK_SIZE = 10f;

    @Override
    protected String getHelp()
    {
        return "Places " + (int) STACK_SIZE + " of every weapon into the "
                + STATION + ", or another station specified by the player.\n"
                + "Warning: if the station is not found, this command will "
                + "instead place one of every weapon into your "
                + "fleet's inventory!";
    }

    @Override
    protected String getSyntax()
    {
        return "allweapons <optionalStation>";
    }

    @Override
    public boolean runCommand(String args)
    {
        SectorEntityToken target;

        float amount = STACK_SIZE;
        float total = 0;

        if (args == null || args.isEmpty())
        {
            args = STATION;
        }

        target = ((StarSystemAPI) getLocation()).getEntityByName(args);

        if (target == null)
        {
            showMessage(args + " not found! Defaulting to player cargo.");
            target = Global.getSector().getPlayerFleet();
            amount = 1f;
        }

        CargoAPI cargo = target.getCargo();
        List allWeapons = Global.getSector().getAllWeaponIds();

        for (int x = 0; x < allWeapons.size(); x++)
        {
            cargo.addItems(CargoItemType.WEAPONS, allWeapons.get(x), amount);
            total += amount;
        }

        showMessage("Added " + total + " items to " + target.getFullName() + ".");
        return true;
    }
}
