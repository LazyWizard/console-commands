package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

public class ShowLoc implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context.isInCampaign())
        {
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            Vector2f loc = player.getLocation();
            String systemName = player.getContainingLocation().getName();
            Console.showMessage("Current location: {" + loc.x + ", " + loc.y
                    + "} in " + systemName);
        }
        else
        {
            Vector2f loc = Global.getCombatEngine().getPlayerShip().getLocation();
            Console.showMessage("Coords: {" + loc.x + ", " + loc.y + "}");
        }

        return CommandResult.SUCCESS;
    }
}
