package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FogOfWarAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatPlugin;
import org.lazywizard.console.cheatmanager.CombatCheatManager;

import java.util.List;

public class Reveal implements BaseCommand
{
    private static final String CHEAT_ID = "lw_console_reveal";

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        // In campaign, just give the player fleet a massive sensor boost
        if (context.isInCampaign())
        {
            final MutableFleetStatsAPI stats = Global.getSector().getPlayerFleet().getStats();
            final StatBonus rangeMod = stats.getSensorRangeMod();
            final StatBonus strengthMod = stats.getSensorStrengthMod();
            if (rangeMod.getFlatBonus(CHEAT_ID) != null)
            {
                rangeMod.unmodify(CHEAT_ID);
                strengthMod.unmodify(CHEAT_ID);
                Console.showMessage("Sensors returned to normal.");
                return CommandResult.SUCCESS;
            }

            rangeMod.modifyFlat(CHEAT_ID, 999_999f, "Console");
            strengthMod.modifyFlat(CHEAT_ID, 999_999f, "Console");
            Console.showMessage("Sensor strength maximized.");
            return CommandResult.SUCCESS;
        }

        // In combat, toggles a cheat plugin to handle per-frame reveals
        if (CombatCheatManager.isEnabled(CHEAT_ID))
        {
            CombatCheatManager.disableCheat(CHEAT_ID);
            Console.showMessage("Fog of war re-enabled.");
            return CommandResult.SUCCESS;
        }
        else
        {
            CombatCheatManager.enableCheat(CHEAT_ID, "Reveal Map", new RevealPlugin(), null);
            Console.showMessage("Fog of war disabled.");
            return CommandResult.SUCCESS;
        }
    }

    private static class RevealPlugin extends CheatPlugin
    {
        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            final CombatEngineAPI engine = Global.getCombatEngine();
            final FogOfWarAPI fow = engine.getFogOfWar(FleetSide.PLAYER.ordinal());
            final float radius = Math.max(engine.getMapWidth(), engine.getMapHeight());
            fow.revealAroundPoint(this, 0f, 0f, radius);
        }

        @Override
        public boolean runWhilePaused()
        {
            return true;
        }
    }
}
