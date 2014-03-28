package org.lazywizard.console;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

/**
 * Contains basic mod constants, common error messages, and the IDs of sector
 * persistent data that core commands use.
 * <p>
 * @author LazyWizard
 * @since 2.0
 */
public class CommonStrings
{
    //<editor-fold defaultstate="collapsed" desc="Mod implementation constants">
    /**
     * The ID of this mod as defined in mod_info.json, used for loading
     * commands.
     * <p>
     * @since 2.0
     */
    public static final String MOD_ID = "lw_console";
    /**
     * The path to console_settings.csv.
     * <p>
     * @since 2.0
     */
    public static final String SETTINGS_PATH = "data/console/console_settings.json";
    /**
     * The path to commands.csv, used for loading commands.
     * <p>
     * @since 2.0
     */
    public static final String CSV_PATH = "data/console/commands.csv";
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Common messages">
    /**
     * The message the console shows when it's first summoned.
     * <p>
     * @since 2.0
     */
    public static final String INPUT_QUERY = "Enter a command, or 'help' for a list of valid commands.";
    /**
     * The error message displayed by core commands when a campaign-only command
     * is used elsewhere.
     * <p>
     * @since 2.0
     */
    public static final String ERROR_CAMPAIGN_ONLY = "Error: This command is campaign-only.";
    /**
     * The error message displayed by core commands when a combat-only command
     * is used elsewhere.
     * <p>
     * @since 2.0
     */
    public static final String ERROR_COMBAT_ONLY = "Error: This command is combat-only.";
    /**
     * The error message displayed by core commands when a mission-only command
     * is used elsewhere.
     * <p>
     * @since 2.0
     */
    public static final String ERROR_MISSION_ONLY = "Error: This command is mission-only.";
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Persistent data IDs used by default commands">
    /**
     * A recommended prefix for keys in sector persistent data to avoid
     * collision with any existing keys.
     * <p>
     * @since 2.0
     */
    public static final String DATA_PREFIX = MOD_ID + "_";

    /**
     * The persistent data ID of the {@link SectorEntityToken} used by the Home
     * command.
     * <p>
     * @since 2.0
     */
    public static final String DATA_HOME_ID = DATA_PREFIX + "Home";

    /**
     * The persistent data ID of the {@link CargoAPI} used by the Storage
     * command.
     * <p>
     * @since 2.0
     */
    public static final String DATA_STORAGE_ID = DATA_PREFIX + "Storage";
    //</editor-fold>

    private CommonStrings()
    {
    }
}
