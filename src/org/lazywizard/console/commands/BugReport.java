package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

import static org.lazywizard.console.ext.SystemInfo.*;

public class BugReport implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        final StringBuilder modData = new StringBuilder(1024);
        // All of these methods can be found in BugReportExt.kt and its subfiles
        modData.append(" System info:\n---------------\n")
                .append(getGameVersionString())
                .append(getDisplayString())
                .append(getPlatformString())
                .append(getRAMString())
                .append(getLaunchArgsString());

        modData.append("\n Graphics card info:\n---------------------\n").append(getGPUString());

        modData.append("\n Active mod list:\n------------------\n");
        final List<ModSpecAPI> allMods = Global.getSettings().getModManager().getEnabledModsCopy(),
                regularMods = new ArrayList<>(), utilityMods = new ArrayList<>();
        ModSpecAPI tcMod = null;
        for (final ModSpecAPI mod : allMods)
        {
            if (mod.isTotalConversion())
            {
                tcMod = mod;
                continue;
            }

            if (mod.isUtility())
            {
                utilityMods.add(mod);
            }
            else
            {
                regularMods.add(mod);
            }
        }

        if (tcMod != null)
        {
            modData.append("Total conversion: " + getModString(tcMod));
        }

        if (regularMods.isEmpty())
        {
            modData.append("Regular mods: none\n");
        }
        else
        {
            modData.append("Regular mods (" + regularMods.size() + "):\n");
            for (final ModSpecAPI mod : regularMods)
            {
                modData.append(" - " + getModString(mod));
            }
        }

        // Technically impossible since the console alone requires two utility mods, but whatever
        if (utilityMods.isEmpty())
        {
            modData.append("Utility mods: none\n");
        }
        else
        {
            modData.append("Utility mods (" + utilityMods.size() + "):\n");
            for (final ModSpecAPI mod : utilityMods)
            {
                modData.append(" - " + getModString(mod));
            }
        }

        final String modDataString = modData.toString();
        Console.showMessage(modDataString);

        final StringSelection copiedData = new StringSelection(modDataString);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(copiedData, copiedData);
        Console.showMessage("This data has also been copied to the clipboard and is ready to paste into a support thread.");
        return CommandResult.SUCCESS;
    }
}
