package org.lazywizard.console.commands;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.lang.management.ManagementFactory;

public class DumpHeap implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!"confirm".equalsIgnoreCase(args))
        {
            Console.showMessage("Warning: dumping memory takes time and can use multiple gigabytes of disk space" +
                    " for a heavily modded game. Enter \"dumpheap confirm\" to continue.\n");
            return CommandResult.SUCCESS;
        }

        try
        {
            final String filename = "console_heapdump_" + System.nanoTime() + ".hprof";
            ManagementFactory.newPlatformMXBeanProxy(
                    ManagementFactory.getPlatformMBeanServer(),
                    "com.sun.management:type=HotSpotDiagnostic",
                    HotSpotDiagnosticMXBean.class).dumpHeap(filename, true);
            Console.showMessage("Memory dumped to '" + System.getProperty("user.dir")
                    + System.getProperty("file.separator") + filename + "'.\n");
            return CommandResult.SUCCESS;
        }
        catch (Exception ex)
        {
            Console.showException("Failed to dump memory: ", ex);
            return CommandResult.ERROR;
        }
    }
}
