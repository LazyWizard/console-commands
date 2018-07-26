package data.console.commands.intermediate;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Dogpile implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Give all hostile fleets in the current system an intercept order on the player
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        for (CampaignFleetAPI fleet : player.getContainingLocation().getFleets())
        {
            if (fleet.getFaction().isHostileTo(player.getFaction()))
            {
                fleet.getAI().addAssignmentAtStart(FleetAssignment.INTERCEPT, player,
                        15f, "Experiencing murderous rage", null);
            }
        }

        Console.showMessage("Hostile fleets summoned.");
        return CommandResult.SUCCESS;
    }
}
