package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class CopyModList implements BaseCommand
{
    private static String getModString(ModSpecAPI mod)
    {
        return mod.getName() + " " + mod.getVersion() + " by " + mod.getAuthor() + "\n";
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        final StringBuilder modData = new StringBuilder(256);
        modData.append(" Active mod list:\n------------------\n");
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

        final StringSelection copiedData = new StringSelection(modData.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(copiedData, copiedData);
        Console.showMessage("Active mod list has been copied to the clipboard and is ready to paste.");
        return CommandResult.SUCCESS;
    }
}
