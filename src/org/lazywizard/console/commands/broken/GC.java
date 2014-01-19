package org.lazywizard.sfconsole.commands;

import org.lazywizard.sfconsole.BaseCommand;

public class GC extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Tell the Java Virtual Machine to run a garbage collection event.";
    }

    @Override
    protected String getSyntax()
    {
        return "gc (no arguments)";
    }

    @Override
    protected boolean isUseableInCombat()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        System.gc();
        return true;
    }
}
