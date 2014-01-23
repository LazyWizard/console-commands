package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import java.lang.ref.WeakReference;
import java.util.List;
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
        if (context == CommandContext.CAMPAIGN)
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        InfiniteAmmoPlugin tmp;
        if (plugin == null || plugin.get() == null)
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

    private static class InfiniteAmmoPlugin implements EveryFrameCombatPlugin
    {
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
            }
        }

        @Override
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
        }
    }
}
