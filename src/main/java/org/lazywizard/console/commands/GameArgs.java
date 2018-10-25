package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.lang.management.ManagementFactory;

public class GameArgs implements BaseCommand
{
    public static String getLaunchArgs()
    {
        final StringBuilder cmdLine = new StringBuilder();
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments())
        {
            cmdLine.append(arg).append(" ");
        }
        return cmdLine.toString();
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        final String cmdLine = getLaunchArgs();
        Console.showIndentedMessage("Command-line arguments used to launch Starsector:", cmdLine, 3);
        final StringSelection copiedData = new StringSelection(cmdLine);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(copiedData, copiedData);
        Console.showMessage("These arguments have also been copied to the system clipboard.");
        return CommandResult.SUCCESS;
    }
}
