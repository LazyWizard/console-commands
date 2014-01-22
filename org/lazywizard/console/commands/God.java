package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import java.lang.ref.WeakReference;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class God implements BaseCommand
{
    private static WeakReference<GodPlugin> plugin;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.CAMPAIGN)
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        GodPlugin tmp;
        if (plugin == null || plugin.get() == null)
        {
            tmp = new GodPlugin();
            plugin = new WeakReference<>(tmp);
            Global.getCombatEngine().addPlugin(tmp);
            Console.showMessage("God mode enabled.");
        }
        else
        {
            tmp = plugin.get();
            plugin.clear();
            tmp.godMode = false;
            Console.showMessage("God mode disabled.");
        }

        return CommandResult.SUCCESS;
    }

    private static class GodPlugin implements EveryFrameCombatPlugin
    {
        private boolean godMode = true;
        private CombatEngineAPI engine;

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            for (ShipAPI ship : engine.getShips())
            {
                if (ship.isHulk() || ship.isShuttlePod())
                {
                    continue;
                }

                if (ship.getOwner() == FleetSide.PLAYER.ordinal())
                {
                    if (godMode)
                    {
                        ship.getMutableStats().getHullDamageTakenMult().modifyMult("console_god", 0f);
                        ship.getMutableStats().getEmpDamageTakenMult().modifyMult("console_god", 0f);
                    }
                    else
                    {
                        ship.getMutableStats().getHullDamageTakenMult().unmodify("console_god");
                        ship.getMutableStats().getEmpDamageTakenMult().unmodify("console_god");
                    }
                }
            }

            if (!godMode)
            {
                engine.removePlugin(this);
            }
        }

        @Override
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
        }
    }
}
