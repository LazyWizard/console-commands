package org.lazywizard.console;

/**
 * The basic interface all console commands must implement.
 * <p>
 * @author LazyWizard
 * @since 2.0
 */
public interface BaseCommand
{
    /**
     * Represents the success status of a command. Returned by
     * {@link BaseCommand#runCommand(String, BaseCommand.CommandContext)}.
     * <p>
     * @since 2.0
     */
    public static enum CommandResult
    {
        /**
         * Command ran successfully.
         * <p>
         * @since 2.0
         */
        SUCCESS,
        /**
         * Something went wrong while executing the command.
         * <p>
         * @since 2.0
         */
        ERROR,
        /**
         * Command had the wrong arguments passed in.
         * <p>
         * @since 2.0
         */
        BAD_SYNTAX,
        /**
         * Command was used in the wrong context (ex: entering a campaign-only
         * command in a mission).
         * <p>
         * @since 2.0
         */
        WRONG_CONTEXT
    }

    /**
     * Represents what screen the player was on when they used the command.
     * <p>
     * @since 2.0
     */
    public static enum CommandContext
    {
        /**
         * Command was entered on the campaign map.
         * <p>
         * @since 2.0
         */
        CAMPAIGN_MAP,
        /**
         * Command was entered during a battle in the campaign (doesn't include
         * simulation battles).
         * <p>
         * @since 2.0
         */
        COMBAT_CAMPAIGN,
        /**
         * Command was entered during a mission.
         * <p>
         * @since 2.0
         */
        COMBAT_MISSION,
        /**
         * Currently unused due to API limitations. Simulations will call
         * {@link CommandContext#COMBAT_MISSION} instead.
         * <p>
         * @since 2.0
         */
        COMBAT_SIMULATION
    }

    /**
     * Executes your command. Called when the player enters your command.
     *
     * @param args    The arguments passed into this command. Will be an empty
     *                string if no arguments were entered.
     * @param context Where this command was called from (campaign, combat,
     *                mission, simulation, etc).
     * <p>
     * @return A {@link CommandResult} describing the result of execution.
     * <p>
     * @since 2.0
     */
    public CommandResult runCommand(String args, CommandContext context);
}
