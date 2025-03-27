package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// TODO: A lot of these static methods should be moved to LazyLib's ModUtils
public class ModInfo implements BaseCommand
{
    private static final Logger Log = Global.getLogger(ModInfo.class);

    // Only works with core CSVs
    private static List<String> getRowsAddedByMod(String csvPath, String column, ModSpecAPI mod)
    {
        final String modDir = mod.getDirName() + "/" + csvPath;
        final JSONArray csv;
        try
        {
            csv = Global.getSettings().getMergedSpreadsheetDataForMod(column, csvPath, "starsector-core");
        }
        catch (Exception ex)
        {
            // Not actually an error, just means that mod doesn't override the core CSV
            Log.error("Failed to open " + csvPath, ex);
            return Collections.emptyList();
        }

        final List<String> added = new ArrayList<>(csv.length());
        try
        {
            for (int i = 0; i < csv.length(); i++)
            {
                final JSONObject row = csv.getJSONObject(i);

                // Skip empty rows
                final String id = row.getString(column);
                if (id.isEmpty())
                {
                    continue;
                }

                // Skip rows not from that mod
                final String source = row.getString("fs_rowSource");
                //System.out.print(id + ": " + modDir + " vs " + source + ", ");
                if (source == null || !source.endsWith(modDir))
                {
                    //System.out.println("MISMATCH");
                    continue;
                }

                //System.out.println("MATCH");
                added.add(id);
            }
        }
        catch (Exception ex)
        {
            // Okay, this one's actually an error
            Log.error("Failed to parse " + csvPath, ex);
            return Collections.emptyList();
        }

        Collections.sort(added, String.CASE_INSENSITIVE_ORDER);
        return added;
    }

    public static List<String> getShipsAddedByMod(ModSpecAPI mod)
    {
        return getRowsAddedByMod("data/hulls/ship_data.csv", "id", mod);
    }

    public static List<String> getWingsAddedByMod(ModSpecAPI mod)
    {
        return getRowsAddedByMod("data/hulls/wing_data.csv", "id", mod);
    }

    public static List<String> getWeaponsAddedByMod(ModSpecAPI mod)
    {
        return getRowsAddedByMod("data/weapons/weapon_data.csv", "id", mod);
    }

    public static List<String> getCommoditiesAddedByMod(ModSpecAPI mod)
    {
        return getRowsAddedByMod("data/campaign/commodities.csv", "id", mod);
    }

    public static List<String> getSpecialItemsAddedByMod(ModSpecAPI mod)
    {
        return getRowsAddedByMod("data/campaign/special_items.csv", "id", mod);
    }

    public static List<String> getIndustriesAddedByMod(ModSpecAPI mod)
    {
        return getRowsAddedByMod("data/campaign/industries.csv", "id", mod);
    }

    public static List<String> getMarketConditionsAddedByMod(ModSpecAPI mod)
    {
        return getRowsAddedByMod("data/campaign/market_conditions.csv", "id", mod);
    }

    private static String pad(int length, char padWith)
    {
        return new String(new char[length]).replace('\0', padWith);
    }

    private static String implodeOrNone(Collection<String> toImplode, int indent)
    {
        // Because an empty string looks ugly
        if (toImplode.isEmpty())
        {
            return ": none";
        }

        // Single entry probably fits on its own line
        if (toImplode.size() == 1)
        {
            return ": " + toImplode.iterator().next();
        }

        // Why waste CPU cycles when you don't have to?
        if (indent <= 0)
        {
            return " (" + toImplode.size() + "):\n" + CollectionUtils.implode(toImplode);
        }

        return " (" + toImplode.size() + "):\n" + CommandUtils.indent(CollectionUtils.implode(toImplode), indent);
    }

    public static String getInfo(ModSpecAPI mod)
    {
        final ModManagerAPI manager = Global.getSettings().getModManager();
        final String modId = mod.getId();

        final StringBuilder sb = new StringBuilder(2048);
        sb.append(" Report for mod ").append(modId).append(":\n")
                .append(pad(sb.length(), '-'))
                .append("\n - Display name: ").append(mod.getName())
                .append("\n - Author: ").append(mod.getAuthor())
                .append("\n - Description:\n")
                .append(CommandUtils.indent(mod.getDesc(), 5))
                .append("\n - Version: ").append(mod.getVersion())
                .append("\n - Total conversion: ").append(mod.isTotalConversion())
                .append("\n - Utility: ").append(mod.isUtility())
                .append("\n - Game version: ").append(mod.getGameVersion())
                .append("\nOther information:")
                .append("\n - Enabled: ").append(manager.isModEnabled(modId))
                .append("\n - Mod plugin: ").append(mod.getModPluginClassName())
                .append("\n - Location: ").append(mod.getPath())
                .append("\n - Jars").append(implodeOrNone(mod.getJars(), 5))
                .append("\n - Overrides").append(implodeOrNone(mod.getFullOverrides(), 5));

        if (manager.isModEnabled(modId))
        {
            final List<String> addedShips = getShipsAddedByMod(mod),
                    addedWings = getWingsAddedByMod(mod),
                    addedWeapons = getWeaponsAddedByMod(mod),
                    addedCommodities = getCommoditiesAddedByMod(mod),
                    addedSpecials = getSpecialItemsAddedByMod(mod),
                    addedIndustries = getIndustriesAddedByMod(mod),
                    addedConditions = getMarketConditionsAddedByMod(mod);
            sb.append("\nAdded or replaced content:")
                    .append("\n - Hulls").append(implodeOrNone(addedShips, 5))
                    .append("\n - Wings").append(implodeOrNone(addedWings, 5))
                    .append("\n - Weapons").append(implodeOrNone(addedWeapons, 5))
                    .append("\n - Commodities").append(implodeOrNone(addedCommodities, 5))
                    .append("\n - Special items").append(implodeOrNone(addedSpecials, 5))
                    .append("\n - Industries").append(implodeOrNone(addedIndustries, 5))
                    .append("\n - Market conditions").append(implodeOrNone(addedConditions, 5));
        }

        return sb.toString();
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        final List<ModSpecAPI> mods = Global.getSettings().getModManager().getAvailableModsCopy();
        final List<String> modIds = new ArrayList<>(mods.size());
        for (ModSpecAPI mod : mods)
        {
            modIds.add(mod.getId());
        }

        final String match = CommandUtils.findBestStringMatch(args, modIds);
        if (match == null)
        {
            Console.showMessage("No mod found with id \"" + args
                    + "\"! Use 'list mods' to find the proper id!");
            return CommandResult.ERROR;
        }

        final ModSpecAPI mod = Global.getSettings().getModManager().getModSpec(match);
        if (mod == null)
        {
            Console.showMessage("Something went wrong!");
            return CommandResult.ERROR;
        }

        Console.showMessage(getInfo(mod));
        return CommandResult.SUCCESS;
    }
}
