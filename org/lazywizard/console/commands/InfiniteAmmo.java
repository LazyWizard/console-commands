package org.lazywizard.console.commands;

import java.lang.ref.WeakReference;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class InfiniteAmmo implements BaseCommand
{
    private static WeakReference<InfiniteAmmoPlugin> plugin;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        InfiniteAmmoPlugin tmp;
        if (plugin == null || plugin.get() == null
                || plugin.get().engine != Global.getCombatEngine())
        {
            tmp = new InfiniteAmmoPlugin();
            plugin = new WeakReference<>(tmp);
            Global.getCombatEngine().addPlugin(tmp);
            Console.showMessage("Infinite ammo enabled.");
        }
        else
        {
            tmp = plugin.get();
            plugin.clear();
            tmp.active = false;
            Console.showMessage("Infinite ammo disabled.");
        }

        return CommandResult.SUCCESS;
    }

    private static class InfiniteAmmoPlugin extends BaseEveryFrameCombatPlugin
    {
        private final IntervalUtil nextCheck = new IntervalUtil(0.1f, 0.1f);
        private boolean active = true, firstRun = true;
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

                    for (WeaponAPI wep : ship.getAllWeapons())
                    {
                        wep.resetAmmo();
                    }

                    final ShipSystemAPI system = ship.getSystem();
                    if (system != null && system.getAmmo() < system.getMaxAmmo())
                    {
                        system.setAmmo(system.getMaxAmmo());
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
