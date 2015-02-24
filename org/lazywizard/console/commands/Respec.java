package org.lazywizard.console.commands;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Respec implements BaseCommand
{
    private static final Set<String> APTITUDE_IDS = new HashSet<>();
    private static final Set<String> SKILL_IDS = new HashSet<>();
    private static boolean isLoaded = false;

    /*private static String filterModPath(String fullPath)
    {
        if (fullPath.startsWith("null/"))
        {
            return "Vanilla";
        }

        try
        {
            String modPath = fullPath.replace("/", "\\");
            modPath = modPath.substring(modPath.lastIndexOf("\\mods\\"));
            modPath = modPath.substring(0, modPath.indexOf('\\', 6)) + "\\";
            return modPath;
        }
        catch (Exception ex)
        {
            Global.getLogger(Respec.class).log(Level.DEBUG,
                    "Failed to reduce modpath '" + fullPath + "'", ex);
            return fullPath;
        }
    }*/

    public static void reloadCSVData() throws JSONException, IOException
    {
        APTITUDE_IDS.clear();
        SKILL_IDS.clear();

        //Global.getLogger(Respec.class).log(Level.DEBUG,
        //        "Loading aptitudes...");
        JSONArray aptitudeData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "id", "data/characters/skills/aptitude_data.csv", "starsector-core");
        for (int x = 0; x < aptitudeData.length(); x++)
        {
            JSONObject tmp = aptitudeData.getJSONObject(x);
            String id = tmp.getString("id");
            //String source = filterModPath(tmp.optString("fs_rowSource", null));
            if (id.isEmpty())
            {
                //Global.getLogger(Respec.class).log(Level.DEBUG,
                //        "Ignoring empty CSV row");
            }
            else
            {
                //Global.getLogger(Respec.class).log(Level.DEBUG,
                //        "Found aptitude \"" + id + "\" from mod " + source);
                APTITUDE_IDS.add(id);
            }
        }

        //Global.getLogger(Respec.class).log(Level.DEBUG,
        //        "Loading skills...");
        JSONArray skillData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "id", "data/characters/skills/skill_data.csv", "starsector-core");
        for (int x = 0; x < skillData.length(); x++)
        {
            JSONObject tmp = skillData.getJSONObject(x);
            String id = tmp.getString("id");
            //String source = filterModPath(tmp.optString("fs_rowSource", null));
            if (id.isEmpty())
            {
                //Global.getLogger(Respec.class).log(Level.DEBUG,
                //        "Ignoring empty CSV row");
            }
            else
            {
                //Global.getLogger(Respec.class).log(Level.DEBUG,
                //        "Found skill \"" + id + "\" from mod " + source);
                SKILL_IDS.add(id);
            }
        }

        Global.getLogger(Respec.class).log(Level.INFO,
                "Found " + APTITUDE_IDS.size() + " aptitudes and "
                + SKILL_IDS.size() + " skills");
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (!isLoaded)
        {
            try
            {
                reloadCSVData();
                isLoaded = true;
            }
            catch (JSONException | IOException ex)
            {
                Console.showException("Failed to load skill/aptitude data!", ex);
                return CommandResult.ERROR;
            }
        }

        Console.showMessage("Performing respec...");

        // Remove aptitudes
        int total;
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        for (String currId : APTITUDE_IDS)
        {
            total = Math.round(player.getAptitudeLevel(currId));
            if (total > 0)
            {
                Console.showMessage(" - removed " + total + " points from aptitude " + currId);
                player.setAptitudeLevel(currId, 0f);
                player.addAptitudePoints(total);
            }
        }

        // Remove skills
        for (String currId : SKILL_IDS)
        {
            total = Math.round(player.getSkillLevel(currId));
            if (total > 0)
            {
                Console.showMessage(" - removed " + total + " points from skill " + currId);
                player.setSkillLevel(currId, 0f);
                player.addSkillPoints(total);
            }
        }

        Console.showMessage("Respec complete.");
        return CommandResult.SUCCESS;
    }
}
