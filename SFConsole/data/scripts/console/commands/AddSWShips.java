package data.scripts.console.commands;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import data.scripts.console.BaseCommand;
import java.util.ArrayList;
import java.util.List;

public class AddSWShips extends BaseCommand
{
    private static final List ships = new ArrayList();
    private static final List wings = new ArrayList();

    static
    {
        ships.add("home_one");
        ships.add("standard_acclimator");
        ships.add("standard_cr90_corvette");
        ships.add("standard_interdictor");
        ships.add("standard_lambda");
        ships.add("standard_star_destroyer");
        ships.add("sw_ardent");
        ships.add("sw_gallofree");
        ships.add("sw_mc80_star_cruiser");
        ships.add("sw_nebulon_b");
        ships.add("sw_sd_interdictor");
        ships.add("sw_victory1");
        ships.add("sw_yt1300");

        wings.add("standard_tie_bomber");
        wings.add("standard_tie_interceptor");
        wings.add("sw_awing");
        wings.add("sw_blastboat");
        wings.add("sw_bwing");
        wings.add("sw_headhunter");
        wings.add("sw_tief");
        wings.add("sw_xwing");
        wings.add("sw_ywing");
    }

    @Override
    protected String getHelp()
    {
        return "Adds all ships from the Star Wars mod to your fleet.";
    }

    @Override
    protected String getSyntax()
    {
        return "addswships (no arguments)";
    }

    @Override
    public boolean runCommand(String args)
    {
        FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        FactoryAPI fact = Global.getFactory();
        FleetMemberAPI tmp;

        for (int x = 0; x < ships.size(); x++)
        {
            tmp = fact.createFleetMember(FleetMemberType.SHIP, (String) ships.get(x));
            fleet.addFleetMember(tmp);
        }

        for (int x = 0; x < wings.size(); x++)
        {
            tmp = fact.createFleetMember(FleetMemberType.FIGHTER_WING, (String) wings.get(x));
            fleet.addFleetMember(tmp);
        }

        showMessage("All Star Wars mod ships have been added.");
        return true;
    }
}
