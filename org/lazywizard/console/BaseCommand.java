package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;

/**
 * The basic interface all console commands must implement.
 * <p>
 * @author LazyWizard
 * @since 2.0
 * @deprecated Inherit {@link ConsoleCommand} instead.
 */
@Deprecated
public interface BaseCommand
{
    /**
     * Represents the success status of a command. Returned by
     * {@link BaseCommand#runCommand(String, BaseCommand.CommandContext)}.
     * <p>
     * @since 2.0
     */
    enum CommandResult
    {
        /**
         * Command ran successfully.
         * <p>
         * @since 2.0
         */
        SUCCESS,
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
        WRONG_CONTEXT,
        /**
         * Something went wrong while executing the command.
         * <p>
         * @since 2.0
         */
        ERROR
    }

    /**
     * Represents what screen the player was on when they used the command.
     * <p>
     * @since 2.0
     */
    enum CommandContext
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
         * Command was entered during a refit simulation battle.
         * <p>
         * @since 2.0
         */
        COMBAT_SIMULATION;

        /**
         * Returns whether this context is on the combat map.
         * <p>
         * @return {@code true} if the game is on the combat map, {@code false}
         *         otherwise.
         * <p>
         * @since 2.4
         */
        public boolean isInCombat()
        {
            return (this != CAMPAIGN_MAP);
        }

        /**
         * Returns whether this context is on the campaign map.
         * <p>
         * @return {@code true} if the game is on the campaign map,
         *         {@code false} otherwise.
         * <p>
         * @since 2.4
         */
        public boolean isInCampaign()
        {
            return (this == CAMPAIGN_MAP);
        }

        /**
         * Returns whether the player is in campaign mode, including in campaign
         * battles (even refit simulation battles).
         * <p>
         * @return {@code true} if the player is on the campaign map, in a
         *         campaign battle, or running a simulation in a campaign refit
         *         screen.
         * <p>
         * @since 3.0
         */
        public boolean isCampaignAccessible()
        {
            if (isInCampaign())
            {
                return true;
            }

            final CombatEngineAPI engine = Global.getCombatEngine();
            return engine != null && (engine.isInCampaign() || engine.isInCampaignSim());
        }
    }

    /**
     * Called when the player enters your command.
     *
     * @param args    The arguments passed into this command. Will be an empty
     *                string if no arguments were entered. Trailing whitespace
     *                is trimmed automatically.
     * @param context Where this command was called from (campaign, combat,
     *                mission, simulation, etc).
     * <p>
     * @return A {@link CommandResult} describing the result of execution.
     * <p>
     * @since 2.0
     */
    CommandResult runCommand(String args, CommandContext context);
}
