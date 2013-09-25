package org.lazywizard.sfconsole.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lazywizard.sfconsole.BaseCommand;
import org.lazywizard.sfconsole.Console;

public class SpawnFleet extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Spawns the designated fleet near the player character. The"
                + " fleet id must exist in the .faction file of the faction"
                + " that was entered as an argument.";
    }

    @Override
    protected String getSyntax()
    {
        return "spawnfleet <faction> <fleetID>";
    }

    @Override
    public boolean runCommand(String args)
    {
        String[] tmp = args.split(" ");

        if (tmp.length != 2)
        {
            showSyntax();
            return false;
        }

        String faction = tmp[0];
        String fleet = tmp[1];

        CampaignFleetAPI toSpawn = Global.getSector().createFleet(faction, fleet);
        Global.getSector().getCurrentLocation().spawnFleet(
                Global.getSector().getPlayerFleet(), 50, 50, toSpawn);

        Console.showMessage("Fleet '" + fleet + "' successfully spawned!");

        return true;
    }
}
