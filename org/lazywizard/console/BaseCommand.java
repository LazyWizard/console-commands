package org.lazywizard.console;

public interface BaseCommand
{
    public static enum CommandResult
    {
        /**
         * Command ran successfully.
         */
        SUCCESS,
        /**
         * Something went wrong while executing the command.
         */
        ERROR,
        /**
         * Command was used in the wrong context (ex: entering a campaign-only
         * command in a mission).
         */
        WRONG_CONTEXT,
        /**
         * Command had the wrong arguments passed in.
         */
        BAD_SYNTAX
    }

    public static enum CommandContext
    {
        /**
         * Command was entered on the campaign map.
         */
        CAMPAIGN,
        /**
         * Command was entered during a battle in the campaign (doesn't include
         * simulation battles).
         */
        COMBAT_CAMPAIGN,
        /**
         * Command was entered during a mission.
         */
        COMBAT_MISSION,
        /**
         * Currently unused due to API limitations. Simulations will call
         * {@link CommandContext#COMBAT_MISSION} instead.
         */
        COMBAT_SIMULATION
    }

    /**
     * Executes this command.
     *
     * @param args    The arguments passed into this command. Will be an empty
     *                string if no arguments were entered.
     * @param context Whether this command was run in combat or on the campaign
     *                map.
     * <p>
     * @return A {@link CommandResult} describing the result of execution.
     * <p>
     * @since 2.0
     */
    public CommandResult runCommand(String args, CommandContext context);
}
