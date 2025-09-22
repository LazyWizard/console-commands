package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.lazywizard.console.*;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class Jump implements BaseCommandWithSuggestion
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
            List<StarSystemAPI> systems = Global.getSector().getStarSystems();
            List<String> systemNames = new ArrayList<>(systems.size());

            // Player has used SetHome command
            if (Global.getSector().getPersistentData()
                    .get(CommonStrings.DATA_HOME_ID) != null)
            {
                systemNames.add("Home");
            }

            // This check isn't necessary, but just to future-proof this code...
            if (Global.getSector().getHyperspace() != null)
            {
                systemNames.add("Hyperspace");
            }

            // Add the names of every star system currently loaded
            for (StarSystemAPI system : systems)
            {
                systemNames.add(system.getName().substring(0,
                        system.getName().lastIndexOf(" Star System")));
            }

            Console.showMessage("Available destinations:\n"
                    + CollectionUtils.implode(systemNames));
            return CommandResult.SUCCESS;
        }

        if ("home".equalsIgnoreCase(args))
        {
            return (new Home().runCommand("", context));
        }

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        SectorEntityToken destination = null;

        if ("hyperspace".equalsIgnoreCase(args))
        {
            if (player.getContainingLocation().isHyperspace())
            {
                Console.showMessage("You successfully traveled nowhere.");
                return CommandResult.SUCCESS;
            }

            if (player.getContainingLocation() instanceof StarSystemAPI)
            {
                StarSystemAPI system = (StarSystemAPI) player.getContainingLocation();
                destination = system.getHyperspaceAnchor();
            }

            if (destination == null)
            {
                Console.showMessage("Couldn't determine a route to hyperspace!");
                destination = Global.getSector().getHyperspace().createToken(0f, 0f);
            }
        }
        else
        {
            StarSystemAPI system = CommandUtils.findBestSystemMatch(args);
            if (system == null)
            {
                Console.showMessage("No system found with the name '" + args + "'!");
                return CommandResult.ERROR;
            }

            if (system == player.getContainingLocation())
            {
                Console.showMessage("You successfully traveled nowhere.");
                return CommandResult.SUCCESS;
            }

            destination = system.getStar();
            if (destination == null)
            {
                destination = system.createToken(0f, 0f);
            }
            else
            {
                // Don't appear within a star's corona effect!
                final PlanetAPI star = (PlanetAPI) destination;
                final float distance = 2f * (star.getRadius()
                        + star.getSpec().getCoronaSize()) + 200f;
                final Vector2f offset = MathUtils.getRandomPointOnCircumference(null, distance);
                destination = system.createToken(offset.x, offset.y);
            }
        }

        //Global.getSector().doHyperspaceTransition(player, player,
        //        new JumpPointAPI.JumpDestination(destination, "Jumping to " + args));
        player.getContainingLocation().removeEntity(player);
        destination.getContainingLocation().addEntity(player);
        Global.getSector().setCurrentLocation(destination.getContainingLocation());
        player.setLocation(destination.getLocation().x,
                destination.getLocation().y);
        player.setNoEngaging(2.0f);
        player.clearAssignments();
        player.addAssignment(FleetAssignment.GO_TO_LOCATION, destination, 1f);
        Console.showMessage("Jumped to " + destination.getContainingLocation().getName() + ".");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0 || !context.isInCampaign()) return new ArrayList<>();
        return Global.getSector().getStarSystems().stream().map(it -> it.getBaseName()).toList();
    }
}
