package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

public class ShowLoc implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.CAMPAIGN_MAP)
        {
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            Vector2f loc = player.getLocation();
            LocationAPI syatem = player.getContainingLocation();
            String systemName = (syatem.isHyperspace() ? "Hyperspace"
                    : ((StarSystemAPI) syatem).getName());
            Console.showMessage("Current location: {" + loc.x + ", " + loc.y
                    + "} in " + systemName);
        }
        else
        {
            ShipAPI player = Global.getCombatEngine().getPlayerShip();
            Vector2f loc = player.getLocation();
            Console.showMessage("Coords: {" + loc.x + ", " + loc.y + "}");
        }

        return CommandResult.SUCCESS;
    }
}
