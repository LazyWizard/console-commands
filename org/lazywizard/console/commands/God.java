package org.lazywizard.console.commands;

import java.lang.ref.WeakReference;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
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
        if (context == CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        GodPlugin tmp;
        if (plugin == null || plugin.get() == null
                || plugin.get().engine != Global.getCombatEngine())
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
            tmp.active = false;
            Console.showMessage("God mode disabled.");
        }

        return CommandResult.SUCCESS;
    }

    private static class GodPlugin implements EveryFrameCombatPlugin
    {
        private static final String BONUS_ID = "console_god";
        private final IntervalUtil nextCheck = new IntervalUtil(0.5f, 0.5f);
        private boolean active = true, firstRun = true;
        private CombatEngineAPI engine;

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            if (!active)
            {
                for (ShipAPI ship : engine.getShips())
                {
                    if (ship.isHulk() || ship.isShuttlePod()
                            || !(ship.getOwner() == FleetSide.PLAYER.ordinal()))
                    {
                        continue;
                    }

                    ship.getMutableStats().getHullDamageTakenMult().unmodify(BONUS_ID);
                    ship.getMutableStats().getEmpDamageTakenMult().unmodify(BONUS_ID);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(BONUS_ID);
                }

                engine.removePlugin(this);
                return;
            }

            if (engine.isPaused())
            {
                return;
            }

            nextCheck.advance(amount);
            if (firstRun || nextCheck.intervalElapsed())
            {
                firstRun = false;

                for (ShipAPI ship : engine.getShips())
                {
                    if (ship.isHulk() || ship.isShuttlePod()
                            || !(ship.getOwner() == FleetSide.PLAYER.ordinal()))
                    {
                        continue;
                    }

                    ship.getMutableStats().getHullDamageTakenMult()
                            .modifyMult(BONUS_ID, 0f);
                    ship.getMutableStats().getEmpDamageTakenMult()
                            .modifyMult(BONUS_ID, 0f);
                    ship.getMutableStats().getArmorDamageTakenMult()
                            .modifyMult(BONUS_ID, 0.00001f);
                }
            }
        }

        @Override
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
        }

        @Override
        public void renderInWorldCoords(ViewportAPI view)
        {
        }

        @Override
        public void renderInUICoords(ViewportAPI view)
        {
        }
    }
}
