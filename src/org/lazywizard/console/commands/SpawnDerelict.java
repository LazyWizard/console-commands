package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lazywizard.console.CommandUtils.findBestStringMatch;

public class SpawnDerelict implements BaseCommandWithSuggestion
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

        if (!args.toLowerCase().endsWith("_hull"))
        {
            args += "_Hull";
        }

        final String id = findBestStringMatch(args, Global.getSector().getAllEmptyVariantIds());
        if (id == null)
        {
            Console.showMessage("No variant found with id '" + args + "'! Use 'list hulls' for a list of valid IDs.");
            return CommandResult.ERROR;
        }

        final ShipVariantAPI variant = Global.getSettings().getVariant(id);
        if (variant.isFighter() || variant.isStation())
        {
            Console.showMessage("Only ships can be spawned as derelicts!");
            return CommandResult.ERROR;
        }

        final CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        final Vector2f spawnLoc = MathUtils.getRandomPointOnCircumference(player.getLocation(), 500f);
        final DerelictShipData params = new DerelictShipData(new PerShipData(id, ShipCondition.BATTERED), false);
        final SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(
                player.getContainingLocation(), Entities.WRECK, Factions.NEUTRAL, params);
        final ShipRecoverySpecialCreator creator = new ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
        Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        ship.setDiscoverable(true);
        ship.setFixedLocation(spawnLoc.x, spawnLoc.y);
        Console.showMessage("Spawned derelict of hull '" + id + "'.");
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
