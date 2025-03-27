package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class DevMode implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        boolean devMode = Global.getSettings().isDevMode();

        if (devMode && "on".equalsIgnoreCase(args))
        {
            Console.showMessage("Dev mode is already enabled!");
            return CommandResult.SUCCESS;
        }
        else if (!devMode && "off".equalsIgnoreCase(args))
        {
            Console.showMessage("Dev mode is already disabled!");
            return CommandResult.SUCCESS;
        }

        devMode = !devMode;
        Global.getSettings().setDevMode(devMode);
        if (Console.getSettings().getDevModeTogglesDebugFlags()) DebugFlags.setStandardConfig();
        Console.showMessage("Dev mode is now " + (devMode ? "enabled." : "disabled."));
        return CommandResult.SUCCESS;
    }
}
