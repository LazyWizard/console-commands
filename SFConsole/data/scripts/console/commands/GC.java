package data.scripts.console.commands;

import data.scripts.console.BaseCommand;

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
    public boolean runCommand(String args)
    {
        System.gc();
        return true;
    }
}
