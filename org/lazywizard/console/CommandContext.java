package org.lazywizard.console;

public enum CommandContext
{
    /**
     * Command was entered on the campaign map.
     */
    CAMPAIGN,
    /**
     * Command was entered during a battle in the campaign (doesn't include simulation battles).
     */
    COMBAT_CAMPAIGN,
    /**
     * Command was entered during a mission or simulation battle.
     */
    COMBAT_MISSION
}
