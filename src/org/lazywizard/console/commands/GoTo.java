package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lazywizard.console.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

// TODO: Polish system jumps (Meso request)
public class GoTo implements BaseCommandWithSuggestion
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            final Vector2f moveLoc = playerFleet.getMoveDestination();
            if (moveLoc != null)
            {
                playerFleet.setLocation(moveLoc.x, moveLoc.y);
                playerFleet.clearAssignments();
                Console.showMessage("Teleported to move destination.");
                return CommandResult.SUCCESS;
            }

            Console.showMessage("You successfully traveled nowhere.");
            return CommandResult.SUCCESS;
        }

        if ("home".equalsIgnoreCase(args))
        {
            return (new Home().runCommand("", context));
        }

        final SectorEntityToken token = CommandUtils.findTokenInLocation(args,
                Global.getSector().getCurrentLocation());

        if (token == null)
        {
            // Check if the player used this command instead of Jump by mistake
            final StarSystemAPI system = CommandUtils.findBestSystemMatch(args);
            if (system != null || "hyperspace".equalsIgnoreCase(args))
            {
                return (new Jump().runCommand(args, context));
            }

            Console.showMessage("Couldn't find a token by the name '" + args + "'!");
            return CommandResult.ERROR;
        }

        final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (token.equals(playerFleet))
        {
            Console.showMessage("You successfully traveled nowhere.");
            return CommandResult.SUCCESS;
        }

        final Vector2f loc = token.getLocation();
        playerFleet.setLocation(loc.x, loc.y);
        playerFleet.clearAssignments();
        playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, token, 1f);
        Console.showMessage("Teleported to " + token.getFullName() + ".");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0 || !context.isInCampaign()) return new ArrayList<>();

        List<String> blacklist = List.of("magiclib_campaign_trail_custom_entity", "nex_mining_gui_dummy", "luna_campaign_renderer", "orbital_junk");

        return Global.getSector().getCurrentLocation().getAllEntities().stream()
                .filter(it -> !blacklist.contains(it.getCustomEntitySpec() != null ? it.getCustomEntitySpec().getId() : it.getId() ))
                .map(it -> it.getId())
                .toList();
    }
}
