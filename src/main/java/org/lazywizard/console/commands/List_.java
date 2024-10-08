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
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.Console;
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

    private static final boolean PAD_WITH_TAB = true;
    private static final int MAX_PAD_LEN = 4; // Only used if the above is false

    private static String pad(String prefix)
    {
        // LazyLib supports tabs, so let's use those instead
        if (PAD_WITH_TAB)
            return prefix + "\t";

        // Manually calculating things - inaccurate, kind of a pain, kept here as backup
        final int len = MAX_PAD_LEN - (prefix.length() % MAX_PAD_LEN);
        return (len == MAX_PAD_LEN ? prefix : String.format("%s%" + len + "s", prefix, ""));
    }

    private static void wrongContext(String param)
    {
        Console.showMessage("Error: the argument '" + param + "' is campaign-only.");
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        // Only used to update OP of forum thread, not shown in help or syntax
        if (args.equalsIgnoreCase("consolecommands"))
        {
            final List<String> universal = new ArrayList<>(),
                    combat = new ArrayList<>(),
                    campaign = new ArrayList<>();
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
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (String fullId : sector.getAllEmptyVariantIds())
                {
                    final String id = fullId.substring(0, fullId.lastIndexOf("_Hull"));
                    ids.add(pad(id) + "(" + settings.getHullSpec(id).getHullNameWithDashClass() + ")");
                }
                break;
            case "variants":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (String id : settings.getAllVariantIds())
                {
                    ids.add(pad(id) + "(" + settings.getVariant(id).getFullDesignationWithHullName() + ")");
                }
                break;
            case "wings":
            case "fighters":
            case "squadrons":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (String id : sector.getAllFighterWingIds())
                {
                    ids.add(pad(id) + "(" + settings.getFighterWingSpec(id).getWingName() + ")");
                }
                break;
            case "weapons":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (String id : sector.getAllWeaponIds())
                {
                    ids.add(pad(id) + "(" + settings.getWeaponSpec(id).getWeaponName() + ")");
                }
                break;
            case "hullmods":
            case "modspecs":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs())
                {
                    if (!spec.isHidden())
                    {
                        ids.add(pad(spec.getId()) + "(" + spec.getDisplayName() + ")");
                    }
                }
                break;
            case "commodities":
            case "items":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (String id : sector.getEconomy().getAllCommodityIds())
                {
                    ids.add(pad(id) + "(" + settings.getCommoditySpec(id).getName() + ")");
                }
                break;
            case "specials":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs())
                {
                    ids.add(pad(spec.getId()) + "(" + spec.getName() + ")");
                }
                break;
            case "systems":
            case "locations":
                if (!context.isCampaignAccessible())
                {
                    wrongContext(param);
                    return CommandResult.WRONG_CONTEXT;
                }

                newLinePerItem = true;
                ids = new ArrayList<>();
                ids.add(sector.getHyperspace().getId());
                for (LocationAPI location : sector.getStarSystems())
                {
                    ids.add(pad(location.getId()) + "(" + location.getName() + ")");
                }
                break;
            case "factions":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (FactionAPI faction : sector.getAllFactions())
                {
                    ids.add(pad(faction.getId()) + "(" + faction.getDisplayNameLong() +
                            (faction.isShowInIntelTab() ? "" : ", hidden") + ")");
                }
                break;
            case "bases":
                if (!context.isCampaignAccessible())
                {
                    wrongContext(param);
                    return CommandResult.WRONG_CONTEXT;
                }

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
                if (!context.isCampaignAccessible())
                {
                    wrongContext(param);
                    return CommandResult.WRONG_CONTEXT;
                }

                newLinePerItem = true;
                param = "planets in current system";
                ids = new ArrayList<>();
                for (PlanetAPI planet : loc.getPlanets())
                {
                    ids.add(pad(planet.getId()) + "(" + planet.getFullName() + (planet.isStar() ? ", star)" : ")"));
                }
                break;
            case "stations":
                if (!context.isCampaignAccessible())
                {
                    wrongContext(param);
                    return CommandResult.WRONG_CONTEXT;
                }

                newLinePerItem = true;
                param = "stations in current system";
                ids = new ArrayList<>();
                for (SectorEntityToken station : loc.getEntitiesWithTag(Tags.STATION))
                {
                    ids.add(pad(station.getId()) + "(" + station.getFullName() + ")");
                }
                break;
            case "markets":
                if (!context.isCampaignAccessible())
                {
                    wrongContext(param);
                    return CommandResult.WRONG_CONTEXT;
                }

                newLinePerItem = true;
                ids = new ArrayList<>();
                for (MarketAPI market : sector.getEconomy().getMarketsCopy())
                {
                    ids.add(pad(market.getId() + " in " + market.getContainingLocation().getName())
                            + "(" + market.getFaction().getDisplayName() + ", "
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
                    ids.add(pad(id) + ((name == null || name.isEmpty()) ? "" : "(" + name + ")"));
                }
                break;
            case "industries":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (IndustrySpecAPI spec : settings.getAllIndustrySpecs())
                {
                    ids.add(pad(spec.getId()) + "(" + spec.getName() + ")");
                }
                break;
            case "submarkets":
                newLinePerItem = true;
                ids = getSubmarketIds();
                break;
            case "officers":
                if (!context.isCampaignAccessible())
                {
                    wrongContext(param);
                    return CommandResult.WRONG_CONTEXT;
                }

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
        final boolean useFilter = (tmp.length > 1);
        if (useFilter)
        {
            final String filter = args.substring(args.indexOf(' ') + 1);
            param += " containing \"" + filter + "\"";

            for (Iterator<String> iter = ids.iterator(); iter.hasNext(); )
            {
                String id = iter.next().toLowerCase();
                if (!id.contains(filter))
                {
                    iter.remove();
                }
            }

            param = ids.size() + " " + param;
        }
        else
        {
            param = "all " + ids.size() + " " + param;
        }

        // Format and print the list of valid IDs
        Collections.sort(ids, String.CASE_INSENSITIVE_ORDER);
        final String results = CollectionUtils.implode(ids, (newLinePerItem ? "\n" : ", "));
        Console.showIndentedMessage("Listing " + param + ":", results, 3);

        // Notify about the filter feature
        if (!useFilter && ids.size() > 5)
        {
            Console.showMessage("\nThe results can be filtered with 'list " + args + " <filter>' to make " +
                    "finding a specific entry easier (ex: 'list ships hound' or 'list markets hegemony').");
        }

        return CommandResult.SUCCESS;
    }
}
