package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import java.lang.ref.WeakReference;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class InfiniteCR implements BaseCommand
{
    private static WeakReference<InfiniteCRPlugin> plugin;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        InfiniteCRPlugin tmp;
        if (plugin == null || plugin.get() == null
                || plugin.get().engine != Global.getCombatEngine())
        {
            tmp = new InfiniteCRPlugin();
            plugin = new WeakReference<>(tmp);
            Global.getCombatEngine().addPlugin(tmp);
            Console.showMessage("CR timers disabled.");
        }
        else
        {
            tmp = plugin.get();
            plugin.clear();
            tmp.active = false;
            Console.showMessage("CR timers enabled.");
        }

        return CommandResult.SUCCESS;
    }

    private static class InfiniteCRPlugin implements EveryFrameCombatPlugin
    {
        private final IntervalUtil nextCheck = new IntervalUtil(0.5f, 0.5f);
        private boolean active = true;
        private CombatEngineAPI engine;

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            if (!active)
            {
                engine.removePlugin(this);
                return;
            }

            if (engine.isPaused())
            {
                return;
            }

            nextCheck.advance(amount);
            if (nextCheck.intervalElapsed())
            {
                for (ShipAPI ship : engine.getShips())
                {
                    if (ship.isHulk() || ship.isShuttlePod()
                            || !(ship.getOwner() == FleetSide.PLAYER.ordinal()))
                    {
                        continue;
                    }

                    if (ship.losesCRDuringCombat())
                    {
                        ship.setCurrentCR(ship.getCRAtDeployment());
                    }
                }
            }
        }

        @Override
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
        }
    }
}
