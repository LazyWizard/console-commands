package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class BugReport implements BaseCommand
{
    private static String getModString(ModSpecAPI mod)
    {
        return mod.getName() + " " + mod.getVersion() + " by " + mod.getAuthor() + "\n";
    }

    private static String getPlatformString()
    {
        return "Platform: " + LWJGLUtil.getPlatformName() + " (" + (Sys.is64Bit() ? "64" : "32") + "-bit)\n";
    }

    private static String getLaunchArgs()
    {
        final StringBuilder cmdLine = new StringBuilder();
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments())
        {
            cmdLine.append(arg).append(" ");
        }
        return cmdLine.toString();
    }

    private static String getDisplayString()
    {
        final DisplayMode displayMode = Display.getDisplayMode();
        return "Resolution: " + Display.getWidth() + "x" + Display.getHeight() + " ("
                + displayMode.getFrequency() + "hz, " + displayMode.getBitsPerPixel() + "bpp, "
                + (Display.isFullscreen() ? "fullscreen" : "windowed") + ")\n";
    }

    private static String getDriverString()
    {
        return "Graphics driver: " + Display.getAdapter() + "\nDriver version: " + Display.getVersion() + "\n";
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        final StringBuilder modData = new StringBuilder(1024);

        modData.append(" System info:\n---------------\n");
        modData.append("Game version: " + Global.getSettings().getVersionString() + "\n");
        modData.append(getPlatformString());
        //modData.append(getDriverString()); // Almost never works
        modData.append(getDisplayString());
        modData.append("Launch args: " + getLaunchArgs() + "\n");

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
        final StringSelection copiedData = new StringSelection(modDataString);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(copiedData, copiedData);
        Console.showMessage(modDataString);
        Console.showMessage("This data has also been copied to the clipboard and is ready to paste into a support thread.");
        return CommandResult.SUCCESS;
    }
}
