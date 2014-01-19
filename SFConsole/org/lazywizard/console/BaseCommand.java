package org.lazywizard.console;

public interface BaseCommand
{
    /**
     * Executes this command.
     *
     * @param args The arguments passed into this command.
     * @return {@code true} if this command ran successfully, {@code false} otherwise.
     */
    public boolean runCommand(String args);
}
