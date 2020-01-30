package org.lazywizard.console.commands;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.lang.management.ManagementFactory;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DumpHeap implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!"confirm".equalsIgnoreCase(args))
        {
            Console.showMessage("Warning: dumping memory takes time and can use multiple gigabytes of disk space" +
                    " for a heavily modded game.\nWarning: this command is only useful for debugging purposes, and" +
                    " will have no effect on memory usage.\nEnter \"dumpheap confirm\" to continue.");
            return CommandResult.SUCCESS;
        }

        try
        {
            final long startTime = System.nanoTime();
            final Format dateFormat = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");
            final String filename = "console_heapdump__" + dateFormat.format(new Date(System.currentTimeMillis())) + ".hprof";
            ManagementFactory.newPlatformMXBeanProxy(
                    ManagementFactory.getPlatformMBeanServer(),
                    "com.sun.management:type=HotSpotDiagnostic",
                    HotSpotDiagnosticMXBean.class).dumpHeap(filename, true);
            final long totalTime = System.nanoTime() - startTime;
            Console.showMessage("Memory dumped to '" + System.getProperty("user.dir")
                    + System.getProperty("file.separator") + filename + "'.\nTime taken: "
                    + ((double) totalTime / 1_000_000_000.0) + " seconds.");
            return CommandResult.SUCCESS;
        }
        catch (Exception ex)
        {
            Console.showException("Failed to dump memory: ", ex);
            return CommandResult.ERROR;
        }
    }
}
