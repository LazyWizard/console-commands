package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

public class SetRespawn implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        final LocationAPI loc = player.getContainingLocation();
        final Vector2f playerLoc = player.getLocation();

        Global.getSector().setRespawnLocation(loc);
        Global.getSector().getRespawnCoordinates().set(playerLoc);
        Console.showMessage("Respawn location set to {" + playerLoc.x + ", "
                + playerLoc.y + "} in " + loc.getName() + ".");
        return CommandResult.SUCCESS;
    }
}
