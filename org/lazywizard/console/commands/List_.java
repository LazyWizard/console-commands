package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.StringUtils;

public class List_ implements BaseCommand
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

        // Only used to update OP of forum thread, not shown in help or syntax
        if (args.equalsIgnoreCase("consolecommands"))
        {
            final List<String> universal = new ArrayList<>(),
                    combat = new ArrayList<>(), campaign = new ArrayList<>();
            for (String command : CommandStore.getLoadedCommands())
            {
                final List<String> tags = CommandStore.retrieveCommand(command).getTags();
                if (!tags.contains("core"))
                {
                    continue;
                }

                if (tags.contains("console"))
                {
                    universal.add(command);
                }
                else
                {
                    if (tags.contains("campaign"))
                    {
                        campaign.add(command);
                    }

                    if (tags.contains("combat"))
                    {
                        combat.add(command);
                    }
                }
            }

            Collections.sort(universal);
            Collections.sort(campaign);
            Collections.sort(combat);

            final StringBuilder sb = new StringBuilder(1_024);
            sb.append("Universal commands (").append(universal.size()).append("):\n");
            sb.append(CollectionUtils.implode(universal));
            sb.append("\n\nCampaign commands (").append(campaign.size()).append("):\n");
            sb.append(CollectionUtils.implode(campaign));
            sb.append("\n\nCombat commands (").append(combat.size()).append("):\n");
            sb.append(CollectionUtils.implode(combat));

            Console.showMessage(sb.toString());
            return CommandResult.SUCCESS;
        }

        // Get all valid IDs for the specified type
        args = args.toLowerCase();
        String[] tmp = args.split(" ");
        String param = tmp[0];
        final SectorAPI sector = Global.getSector();
        final LocationAPI loc = sector.getCurrentLocation();
        final CampaignFleetAPI player = sector.getPlayerFleet();
        boolean newLinePerItem = false;
        List<String> ids;
        switch (param)
        {
            case "commands":
                ids = new ArrayList<>(CommandStore.getLoadedCommands());
                break;
            case "aliases":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (Map.Entry<String, String> alias : CommandStore.getAliases().entrySet())
                {
                    ids.add(alias.getKey() + " -> " + alias.getValue());
                }
                break;
            case "macros":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (Map.Entry<String, String> macro : RunCode.getMacros().entrySet())
                {
                    ids.add(macro.getKey() + " -> " + macro.getValue());
                }
                break;
            case "tags":
                newLinePerItem = true;
                ids = new ArrayList<>();
                final List<String> tags = CommandStore.getKnownTags();
                Collections.sort(tags);
                for (String tag : tags)
                {
                    final List<String> commandsWithTag = CommandStore.getCommandsWithTag(tag);
                    Collections.sort(commandsWithTag);

                    ids.add(tag + " (" + commandsWithTag.size() + "):\n"
                            + StringUtils.indent(StringUtils.wrapString(
                                    CollectionUtils.implode(commandsWithTag),
                                    Console.getSettings().getMaxOutputLineLength()
                                    - 6), "   "));
                }
                break;
            case "mods":
                newLinePerItem = true;
                param = "enabled mods";
                ids = new ArrayList<>();
                for (ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy())
                {
                    ids.add(mod.getId() + " (" + mod.getName() + " by " + mod.getAuthor()
                            + ", version " + mod.getVersion()
                            + (mod.isTotalConversion() ? ", total conversion" : "")
                            + (mod.isUtility() ? ", utility" : "") + ")");
                }
                break;
            case "ships":
            case "hulls":
                ids = new ArrayList<>();
                for (String id : sector.getAllEmptyVariantIds())
                {
                    ids.add(id.substring(0, id.lastIndexOf("_Hull")));
                }
                break;
            case "variants":
                ids = new ArrayList<>(Global.getSettings().getAllVariantIds());
                break;
            case "wings":
            case "fighters":
            case "squadrons":
                ids = new ArrayList<>(sector.getAllFighterWingIds());
                break;
            case "weapons":
                ids = new ArrayList<>(sector.getAllWeaponIds());
                break;
            case "hullmods":
            case "modspecs":
                ids = new ArrayList<>(AddHullmod.getHullMods());
                break;
            case "commodities":
            case "items":
                ids = new ArrayList<>(sector.getEconomy().getAllCommodityIds());
                break;
            case "systems":
            case "locations":
                ids = new ArrayList<>();
                ids.add(sector.getHyperspace().getId());
                for (LocationAPI location : sector.getStarSystems())
                {
                    ids.add(location.getId());
                }
                break;
            case "factions":
                ids = new ArrayList<>();
                for (FactionAPI faction : sector.getAllFactions())
                {
                    ids.add(faction.getId());
                }
                break;
            case "planets":
                param = "planets in current system";
                ids = new ArrayList<>();
                for (PlanetAPI planet : loc.getPlanets())
                {
                    ids.add(planet.getId() + (planet.isStar() ? " (star)" : ""));
                }
                break;
            case "stations":
                param = "stations in current system";
                ids = new ArrayList<>();
                for (SectorEntityToken station : sector.getCurrentLocation()
                        .getEntitiesWithTag(Tags.STATION))
                {
                    ids.add(station.getId());
                }
                break;
            case "markets":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (MarketAPI market : sector.getEconomy().getMarketsCopy())
                {
                    ids.add(market.getId() + " in " + market.getContainingLocation().getName()
                            + " (" + market.getFaction().getDisplayName() + ", "
                            + (market.getFaction() == null ? "no faction)"
                                    : market.getFaction().getRelationshipLevel(
                                            player.getFaction()).getDisplayName() + ")"));
                }
                break;
            default:
                return CommandResult.BAD_SYNTAX;
        }

        // Support for further filtering results
        if (tmp.length > 1)
        {
            final String filter = args.substring(args.indexOf(' ') + 1);
            param += " starting with \"" + filter + "\"";

            for (Iterator<String> iter = ids.iterator(); iter.hasNext();)
            {
                String id = iter.next().toLowerCase();
                if (!id.startsWith(filter))
                {
                    iter.remove();
                }
            }
        }

        // Format and print the list of valid IDs
        Collections.sort(ids);
        final String results = CollectionUtils.implode(ids, (newLinePerItem ? "\n" : ", "));
        Console.showMessage("Known " + param + " (" + ids.size() + "):\n"
                + StringUtils.indent(StringUtils.wrapString(results,
                        Console.getSettings().getMaxOutputLineLength() - 3), "   "));
        return CommandResult.SUCCESS;
    }
}
