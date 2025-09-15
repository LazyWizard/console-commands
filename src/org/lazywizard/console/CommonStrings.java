package org.lazywizard.console;

import com.fs.starfarer.api.campaign.SectorEntityToken;

/**
 * Contains basic mod constants, common error messages, and the IDs of sector
 * persistent data that core commands use.
 *
 * @author LazyWizard
 * @since 2.0
 */
public class CommonStrings
{
    //<editor-fold defaultstate="collapsed" desc="Mod implementation constants">
    /**
     * The ID of this mod as defined in mod_info.json, used for loading
     * commands.
     *
     * @since 2.0
     */
    public static final String MOD_ID = "lw_console";
    /**
     * The path to console_settings.csv.
     *
     * @since 2.0
     */
    public static final String PATH_SETTINGS = "data/console/console_settings.json";
    /**
     * The path to commands.csv, used for loading commands.
     *
     * @since 2.0
     */
    public static final String PATH_CSV = "data/console/commands.csv";
    /**
     * The path to the config file in common data, automatically generated via the Settings command.
     *
     * @since 3.0
     */
    public static final String PATH_COMMON_DATA = "config/lw_console_settings.json";
    /**
     * The path to runcode_imports.csv, used for setting custom imports for the
     * RunCode command.
     *
     * @since 2.0
     */
    public static final String PATH_RUNCODE_CSV = "data/console/runcode_imports.csv";
    /**
     * The path to runcode_macros.csv, used for setting custom macros for the
     * RunCode command.
     *
     * @since 2.0
     */
    public static final String PATH_RUNCODE_MACROS = "data/console/runcode_macros.csv";
    /**
     * The path to command_listeners.csv, used for listening for and intercepting input.
     *
     * @since 3.0
     */
    public static final String PATH_LISTENER_CSV = "data/console/command_listeners.csv";
    /**
     * Commands with this tag are considered cheats, and will be disabled if the applicable setting is toggled.
     */
    public static final String CHEAT_TAG = "cheat";

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Common messages">
    /**
     * The message the console shows when it's first summoned.
     *
     * @since 2.0
     */
    public static final String INPUT_QUERY = "Enter a command, or 'help' for a list of valid commands.";
    /**
     * The error message displayed by core commands when a campaign-only command
     * is used elsewhere.
     *
     * @since 2.0
     */
    public static final String ERROR_CAMPAIGN_ONLY = "Error: This command is campaign-only.";
    /**
     * The error message displayed by core commands when a market-only command
     * is used elsewhere.
     *
     * @since 3.0
     */
    public static final String ERROR_MARKET_ONLY = "Error: This command can only be used when interacting with a market.";
    /**
     * The error message displayed by core commands when a combat-only command
     * is used elsewhere.
     *
     * @since 2.0
     */
    public static final String ERROR_COMBAT_ONLY = "Error: This command is combat-only.";
    /**
     * The error message displayed by core commands when a mission-only command
     * is used elsewhere.
     *
     * @since 2.0
     */
    public static final String ERROR_MISSION_ONLY = "Error: This command is mission-only.";
    /**
     * The error message displayed by core commands when a simulation-only
     * command is used elsewhere.
     *
     * @since 2.6
     */
    public static final String ERROR_SIMULATION_ONLY = "Error: This command is simulation-only.";
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Persistent data IDs used by default commands">
    /**
     * A recommended prefix for keys in sector persistent data to avoid
     * collision with any existing keys.
     *
     * @since 2.0
     */
    public static final String DATA_PREFIX = MOD_ID + "_";

    /**
     * The persistent data ID of the {@link SectorEntityToken} used by the Home
     * command.
     *
     * @since 2.0
     */
    public static final String DATA_HOME_ID = DATA_PREFIX + "Home";

    /**
     * The persistent data ID of the {@link SectorEntityToken} used by the
     * Storage command.
     *
     * @since 2.0
     */
    public static final String DATA_STORAGE_ID = DATA_PREFIX + "Storage";
    //</editor-fold>

    private CommonStrings()
    {
    }
}
