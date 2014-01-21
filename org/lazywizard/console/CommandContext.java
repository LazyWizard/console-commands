package org.lazywizard.console;

public enum CommandContext
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
