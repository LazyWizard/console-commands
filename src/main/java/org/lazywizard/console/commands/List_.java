package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.bases.LuddicPathBaseIntel;
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseIntel;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lazywizard.console.*;
import org.lazywizard.lazylib.CollectionUtils;

import java.lang.ref.SoftReference;
import java.util.*;

public class List_ implements BaseCommand
{
    private static SoftReference<List<Pair<String, String>>> conditionCache = new SoftReference<>(null);
    private static SoftReference<Set<String>> submarketCache = new SoftReference<>(null);

    public static List<String> getSubmarketIds()
    {
        // Return cached value on subsequent calls (will be culled if available memory gets too low)
        final Set<String> cached = submarketCache.get();
        if (cached != null)
        {
            return new ArrayList<>(cached);
        }

        // If this is the first time calling this method, build the submarket ID cache
        final Set<String> submarkets = new HashSet<>();
        try
        {
            final JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "id", "data/campaign/submarkets.csv", "starsector-core");
            for (int i = 0; i < csv.length(); i++)
            {
                final JSONObject row = csv.getJSONObject(i);
                final String id = row.getString("id");

                // Skip empty rows
                if (!id.isEmpty())
                {
                    submarkets.add(id);
                }
            }
        }
        catch (Exception ex)
        {
            Console.showException("Failed to load submarkets.csv!", ex);
            return Collections.emptyList();
        }

        submarketCache = new SoftReference<>(submarkets);
        return new ArrayList<>(submarkets);
    }

    public static List<Pair<String, String>> getMarketConditionIdsWithNames()
    {
        final List<Pair<String, String>> cached = conditionCache.get();
        if (cached != null)
        {
            return conditionCache.get();
        }

        try
        {
            final List<Pair<String, String>> conditionIdsWithNames = new ArrayList<>();
            final JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "id", "data/campaign/market_conditions.csv", "starsector-core");
            for (int i = 0; i < csv.length(); i++)
            {
                final JSONObject row = csv.getJSONObject(i);
                final String id = row.getString("id");
                final String name = row.optString("name", null);
                conditionIdsWithNames.add(new Pair<>(id, name));
            }

            conditionCache = new SoftReference<>(conditionIdsWithNames);
            return conditionIdsWithNames;
        }
        catch (Exception ex)
        {
            Console.showException("Failed to generate market conditions list!", ex);
            return Collections.emptyList();
        }
    }

    public static List<String> getShipIds()
    {
        final SectorAPI sector = Global.getSector();
        List<String> ids = new ArrayList<>();
        for (String id : sector.getAllEmptyVariantIds())
        {
            ids.add(id.substring(0, id.lastIndexOf("_Hull")));
        }
        return ids;
    }

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

            Collections.sort(universal, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(campaign, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(combat, String.CASE_INSENSITIVE_ORDER);

            final String output = "Universal commands (" + universal.size() + "):\n" +
                    CollectionUtils.implode(universal) +
                    "\n\nCampaign commands (" + campaign.size() + "):\n" +
                    CollectionUtils.implode(campaign) +
                    "\n\nCombat commands (" + combat.size() + "):\n" +
                    CollectionUtils.implode(combat);

            Console.showMessage(output);
            return CommandResult.SUCCESS;
        }

        // Get all valid IDs for the specified type
        args = args.toLowerCase();
        String[] tmp = args.split(" ");
        String param = tmp[0];
        final SettingsAPI settings = Global.getSettings();
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
                Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
                for (String tag : tags)
                {
                    final List<String> commandsWithTag = CommandStore.getCommandsWithTag(tag);
                    Collections.sort(commandsWithTag, String.CASE_INSENSITIVE_ORDER);

                    // Multi-indent is slightly more complicated to avoid word-wrapping issues
                    ids.add(tag + " (" + commandsWithTag.size() + "):\n" + CommandUtils.indent(
                            CollectionUtils.implode(commandsWithTag), 3, "   "));
                }
                break;
            case "mods":
                newLinePerItem = true;
                param = "enabled mods";
                ids = new ArrayList<>();
                for (ModSpecAPI mod : settings.getModManager().getEnabledModsCopy())
                {
                    ids.add(mod.getId() + " (" + mod.getName() + " by " + mod.getAuthor()
                            + ", version " + mod.getVersion()
                            + (mod.isTotalConversion() ? ", total conversion" : "")
                            + (mod.isUtility() ? ", utility" : "") + ")");
                }
                break;
            case "ships":
            case "hulls":
                ids = getShipIds();
                break;
            case "variants":
                ids = new ArrayList<>(settings.getAllVariantIds());
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
            case "specials":
                ids = AddSpecial.getSpecialItemIds();
                break;
            case "systems":
            case "locations":
                newLinePerItem = true;
                ids = new ArrayList<>();
                ids.add(sector.getHyperspace().getId());
                for (LocationAPI location : sector.getStarSystems())
                {
                    ids.add(location.getId() + " (" + location.getName() + ")");
                }
                break;
            case "factions":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (FactionAPI faction : sector.getAllFactions())
                {
                    ids.add(faction.getId() + " (" + faction.getDisplayNameLong() + ")");
                }
                break;
            case "bases":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (IntelInfoPlugin tmpIntel : sector.getIntelManager().getIntel(PirateBaseIntel.class))
                {
                    final PirateBaseIntel intel = (PirateBaseIntel) tmpIntel;
                    ids.add("Pirate Base:\n - Intel: " + intel.getSmallDescriptionTitle() +
                            "\n - Location: " + intel.getMarket().getName() + " in the " +
                            intel.getSystem().getName() + "\n - Target: " + intel.getTarget().getName());
                }
                for (IntelInfoPlugin tmpIntel : sector.getIntelManager().getIntel(LuddicPathBaseIntel.class))
                {
                    final LuddicPathBaseIntel intel = (LuddicPathBaseIntel) tmpIntel;
                    ids.add("Luddic Path Base:\n - Intel: " + intel.getSmallDescriptionTitle() +
                            "\n - Location: " + intel.getMarket().getName() + " in the " +
                            intel.getSystem().getName());
                }
                /*for (IntelInfoPlugin tmpIntel : sector.getIntelManager().getIntel(PirateActivityIntel.class))
                {
                    final PirateActivityIntel intel = (PirateActivityIntel) tmpIntel;
                    ids.add(intel.getSmallDescriptionTitle() +
                            "\n - Source: " + intel.getSource().getMarket().getName() +
                            "\n - Location: " + intel.getSource().getName() +
                            "\n - Target: " + intel.getSystem().getName());
                }*/
                break;
            case "planets":
                newLinePerItem = true;
                param = "planets in current system";
                ids = new ArrayList<>();
                for (PlanetAPI planet : loc.getPlanets())
                {
                    ids.add(planet.getId() + " (" + planet.getFullName() + (planet.isStar() ? ", star)" : ")"));
                }
                break;
            case "stations":
                newLinePerItem = true;
                param = "stations in current system";
                ids = new ArrayList<>();
                for (SectorEntityToken station : loc.getEntitiesWithTag(Tags.STATION))
                {
                    ids.add(station.getId() + " (" + station.getFullName() + ")");
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
            case "conditions":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (Pair<String, String> pair : getMarketConditionIdsWithNames())
                {
                    final String id = pair.one, name = pair.two;
                    ids.add(id + ((name == null || name.isEmpty()) ? "" : " (" + name + ")"));
                }
                break;
            case "industries":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (IndustrySpecAPI spec : settings.getAllIndustrySpecs())
                {
                    ids.add(spec.getId() + " (" + spec.getName() + ")");
                }
                break;
            case "submarkets":
                newLinePerItem = true;
                ids = getSubmarketIds();
                break;
            case "officers":
                newLinePerItem = true;
                param = "officers in fleet";
                ids = new ArrayList<>();
                final List<OfficerDataAPI> officers = player.getFleetData().getOfficersCopy();
                for (int i = 1; i <= officers.size(); i++)
                {
                    final PersonAPI person = officers.get(i - 1).getPerson();
                    ids.add("#" + i + ": " + person.getNameString() + ", " + person.getPersonalityAPI().getDisplayName()
                            + " " + person.getStats().getLevel());
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

            for (Iterator<String> iter = ids.iterator(); iter.hasNext(); )
            {
                String id = iter.next().toLowerCase();
                if (!id.startsWith(filter))
                {
                    iter.remove();
                }
            }
        }

        // Format and print the list of valid IDs
        Collections.sort(ids, String.CASE_INSENSITIVE_ORDER);
        final String results = CollectionUtils.implode(ids, (newLinePerItem ? "\n" : ", "));
        Console.showIndentedMessage(Misc.ucFirst(param) + " (" + ids.size() + "):", results, 3);
        return CommandResult.SUCCESS;
    }
}
