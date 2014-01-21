package org.lazywizard.console;

public interface BaseCommand
{
    /**
     * Executes this command.
     *
     * @param args    The arguments passed into this command. Will be an empty
     *                string if no arguments were entered.
     * @param context Whether this command was run in combat or on the campaign
     *                map.
     * <p>
     * @return {@code true} if this command ran successfully, {@code false}
     *         otherwise.
     * <p>
     * @since 2.0
     */
    public boolean runCommand(String args, CommandContext context);
}
