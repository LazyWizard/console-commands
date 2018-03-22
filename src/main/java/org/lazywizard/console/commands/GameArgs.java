package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.lang.management.ManagementFactory;

public class GameArgs implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        final StringBuilder cmdLine = new StringBuilder();
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments())
        {
            cmdLine.append(arg).append(" ");
        }

        Console.showIndentedMessage("Command-line arguments used to launch Starsector:",
                cmdLine.toString(), 3);
        final StringSelection copiedData = new StringSelection(cmdLine.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(copiedData, copiedData);
        Console.showMessage("These arguments have also been copied to the system clipboard.");
        return CommandResult.SUCCESS;
    }
}
