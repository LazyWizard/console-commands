package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The basic interface all console commands must implement.
 *
 * @author LazyWizard
 * @since 2.0
 */
public interface BaseCommand
{
    /**
     * Represents the success status of a command. Returned by
     * {@link BaseCommand#runCommand(String, BaseCommand.CommandContext)}.
     *
     * @since 2.0
     */
    enum CommandResult
    {
        /**
         * Command ran successfully.
         *
         * @since 2.0
         */
        SUCCESS,
        /**
         * Command had the wrong arguments passed in. Returning this will automatically display the syntax field from
         * your commands.csv, so you usually don't need to include an error message.
         *
         * @since 2.0
         */
        BAD_SYNTAX,
        /**
         * Command was used in the wrong context (ex: entering a campaign-only command in a mission).
         *
         * @since 2.0
         */
        WRONG_CONTEXT,
        /**
         * Something went wrong while executing the command.
         *
         * @since 2.0
         */
        ERROR
    }

    /**
     * Represents what screen the player was on when they used the command.
     *
     * @since 2.0
     */
    enum CommandContext
    {
        /**
         * Command was entered on the campaign map.
         *
         * @since 2.0
         */
        CAMPAIGN_MAP,
        /**
         * Command was entered in a market.
         *
         * @since 3.0
         */
        // TODO: Add to changelog
        CAMPAIGN_MARKET,
        /**
         * Command was entered during a battle in the campaign (doesn't include simulation battles).
         *
         * @since 2.0
         */
        COMBAT_CAMPAIGN,
        /**
         * Command was entered during a mission.
         *
         * @since 2.0
         */
        COMBAT_MISSION,
        /**
         * Command was entered during a refit simulation battle.
         *
         * @since 2.0
         */
        COMBAT_SIMULATION;

        /**
         * Returns whether this context is on the combat map.
         *
         * @return {@code true} if the game is on the combat map, {@code false} otherwise.
         *
         * @since 2.4
         */
        public boolean isInCombat()
        {
            return (this == COMBAT_CAMPAIGN || this == COMBAT_MISSION || this == COMBAT_SIMULATION);
        }

        /**
         * Returns whether this context is on the campaign map.
         *
         * @return {@code true} if the game is on the campaign map, {@code false} otherwise.
         *
         * @since 2.4
         */
        public boolean isInCampaign()
        {
            return (this == CAMPAIGN_MAP || this == CAMPAIGN_MARKET);
        }

        /**
         * Returns whether the player is interacting with a market-containing entity.
         *
         * @return {@code true} if the player is in dialog with a market-containing entity, {@code false} otherwise.
         *
         * @since 3.0
         */
        // TODO: Add to changelog
        public boolean isInMarket()
        {
            return (this == CAMPAIGN_MARKET);
        }

        /**
         * Returns whether the player is in campaign mode, including in campaign battles (even refit simulation
         * battles).
         *
         * @return {@code true} if the player is on the campaign map, in a campaign battle, or running a simulation in a
         *         campaign refit screen.
         *
         * @since 3.0
         */
        public boolean isCampaignAccessible()
        {
            final CombatEngineAPI engine = Global.getCombatEngine();
            return isInCampaign() || (engine != null && (engine.isInCampaign() || engine.isInCampaignSim()));
        }

        /**
         * Returns the {@link SectorEntityToken} the player is in a dialog with, if any.
         *
         * @return The {@link SectorEntityToken} the player is currently in dialog with, or {@code null} if they are not
         *         in a dialog.
         *
         * @since 3.0
         */
        // TODO: Add to changelog
        @Nullable
        public SectorEntityToken getEntityInteractedWith()
        {
            if (!isInCampaign())
            {
                return null;
            }

            final CampaignUIAPI ui = Global.getSector().getCampaignUI();
            return ((ui == null || ui.getCurrentInteractionDialog() == null) ? null
                    : ui.getCurrentInteractionDialog().getInteractionTarget());
        }

        /**
         * Returns the {@link MarketAPI} of the {@link SectorEntityToken) the player is in a dialog with, if any.
         *
         * @return The {@link MarketAPI} of the {@link SectorEntityToken} the player is currently in dialog with, or
         *         {@code null} if they are not in a dialog with a market-containing entity.
         *
         * @since 3.0
         */
        // TODO: Add to changelog
        @Nullable
        public MarketAPI getMarket()
        {
            if (this != CAMPAIGN_MARKET)
            {
                return null;
            }

            final SectorEntityToken interactingWith = getEntityInteractedWith();
            return ((interactingWith == null) ? null : interactingWith.getMarket());
        }
    }

    /**
     * Called when the player enters your command.
     *
     * @param args    The arguments passed into this command. Will be an empty {@link String} if no arguments were
     *                entered.
     * @param context Where this command was called from (campaign, combat, mission, simulation, etc).
     *
     * @return A {@link CommandResult} describing the result of execution.
     *
     * @since 2.0
     */
    CommandResult runCommand(@NotNull String args, @NotNull CommandContext context);
    CommandResult runCommand(String args, CommandContext context);
}
