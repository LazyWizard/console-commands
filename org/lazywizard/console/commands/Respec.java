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

    public static void reloadCSVData() throws JSONException, IOException
    {
        APTITUDE_IDS.clear();
        SKILL_IDS.clear();

        JSONArray aptitudeData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "id", "data/characters/skills/aptitude_data.csv", "starsector-core");
        for (int x = 0; x < aptitudeData.length(); x++)
        {
            JSONObject tmp = aptitudeData.getJSONObject(x);
            String id = tmp.getString("id");

            // Ignore empty CSV rows
            if (!id.isEmpty())
            {
                APTITUDE_IDS.add(id);
            }
        }

        JSONArray skillData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "id", "data/characters/skills/skill_data.csv", "starsector-core");
        for (int x = 0; x < skillData.length(); x++)
        {
            JSONObject tmp = skillData.getJSONObject(x);
            String id = tmp.getString("id");

            // Ignore empty CSV rows
            if (!id.isEmpty())
            {
                SKILL_IDS.add(id);
            }
        }

        Global.getLogger(Respec.class).log(Level.INFO,
                "Found " + APTITUDE_IDS.size() + " aptitudes and "
                + SKILL_IDS.size() + " skills");
        isLoaded = true;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (!isLoaded)
        {
            try
            {
                reloadCSVData();
            }
            catch (JSONException | IOException ex)
            {
                Console.showException("Failed to load skill/aptitude data!", ex);
                return CommandResult.ERROR;
            }
        }

        Console.showMessage("Performing respec...");

        // Refund aptitudes
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

        // Refund skills
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
