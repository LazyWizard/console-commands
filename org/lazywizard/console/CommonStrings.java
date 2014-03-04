package org.lazywizard.console;

public class CommonStrings
{
    // Mod implementation constants
    public static final String MOD_ID = "lw_console";
    public static final String CSV_PATH = "data/console/commands.csv";

    // Common error messages
    public static final String ERROR_CAMPAIGN_ONLY = "Error: This command is campaign-only.";
    public static final String ERROR_COMBAT_ONLY = "Error: This command is combat-only.";
    public static final String ERROR_MISSION_ONLY = "Error: This command is mission-only.";

    // Persistent data IDs used by default commands
    public static final String DATA_PREFIX = MOD_ID + "_";
    public static final String DATA_HOME_ID = DATA_PREFIX + "Home";
    public static final String DATA_STORAGE_ID = DATA_PREFIX + "Storage";

    private CommonStrings()
    {
    }
}
