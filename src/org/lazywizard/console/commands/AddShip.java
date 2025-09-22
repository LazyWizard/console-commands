package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.apache.log4j.Level;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

import static org.lazywizard.console.CommandUtils.*;

public class AddShip implements BaseCommandWithSuggestion
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
            return CommandResult.BAD_SYNTAX;
        }

        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " 1", context);
        }

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        // Redirect fighter wings to AddWing
        if (tmp[0].endsWith("_wing") || tmp[1].endsWith("_wing"))
        {
            return new AddWing().runCommand(args, context);
        }

        // Support for reversed arguments
        int amount;
        if (isInteger(tmp[1]))
        {
            amount = Integer.parseInt(tmp[1]);
        }
        else
        {
            if (!isInteger(tmp[0]))
            {
                return CommandResult.BAD_SYNTAX;
            }

            amount = Integer.parseInt(tmp[0]);
            tmp[0] = tmp[1];
        }

        if (amount <= 0)
        {
            return CommandResult.SUCCESS;
        }

        // Test for variants
        String variant = null;
        for (String id : Global.getSettings().getAllVariantIds())
        {
            if (tmp[0].equalsIgnoreCase(id))
            {
                variant = id;
                break;
            }
        }

        // Test for empty hulls
        if (variant == null)
        {
            final String withHull = tmp[0] + "_Hull";
            for (String id : Global.getSettings().getAllVariantIds())
            {
                if (withHull.equalsIgnoreCase(id))
                {
                    variant = id;
                    break;
                }
            }
        }

        // Before we give up, maybe the .variant file doesn't match the ID?
        if (variant == null)
        {
            try
            {
                variant = Global.getSettings().loadJSON("data/variants/"
                        + tmp[0] + ".variant").getString("variantId");
                Console.showMessage("Warning: variant ID doesn't match"
                        + " .variant filename!", Level.WARN);
            }
            catch (Exception ex)
            {
                Console.showMessage("No ship found with id '" + tmp[0]
                        + "'! Use 'list ships' for a complete list of valid ids.");
                return CommandResult.ERROR;
            }
        }

        // We've finally verified the variant id, now create the actual ship
        FleetMemberAPI ship;
        try
        {
            ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        }
        catch (Exception ex)
        {
            Console.showException("Failed to create variant '" + variant + "'!", ex);
            return CommandResult.ERROR;
        }

        // Redirect fighter wings to AddWing
        if (ship.isFighterWing() || ship.getHullSpec().getHullSize() == HullSize.FIGHTER)
        {
            return new AddWing().runCommand(args, context);
        }

        final FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        for (int i = 0; i < amount; i++)
        {
            ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
            FleetEncounterContext.prepareShipForRecovery(ship,
                    true, true, true,1f, 1f, MathUtils.getRandom());
            fleet.addFleetMember(ship);
        }

        Console.showMessage("Added " + format(amount) + " of ship "
                + ship.getSpecId() + " to player fleet.");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0) return new ArrayList<>();

        ArrayList<String> suggestions = new ArrayList<>();

        suggestions.addAll( Global.getSettings().getAllShipHullSpecs().stream().map(ShipHullSpecAPI::getBaseHullId).distinct().toList() );
        suggestions.addAll( Global.getSettings().getAllVariantIds().stream().filter( it -> !it.endsWith("_Hull")).toList() );

        return suggestions;
    }
}
