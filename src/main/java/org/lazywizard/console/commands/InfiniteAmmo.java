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
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatPlugin;
import org.lazywizard.console.cheatmanager.CheatTarget;
import org.lazywizard.console.cheatmanager.CombatCheatManager;
import org.lazywizard.lazylib.CollectionUtils;

public class InfiniteAmmo implements BaseCommand
{
    private static final String CHEAT_ID = "lw_console_infiniteammo";

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // If no argument is entered:
        // - If cheat is not enabled, enable it for the player only
        // - If cheat is already enabled, disable it
        CheatTarget appliesTo = null;
        if (args.isEmpty())
        {
            if (CombatCheatManager.isEnabled(CHEAT_ID))
            {
                CombatCheatManager.disableCheat(CHEAT_ID);
                Console.showMessage("Infinite ammo disabled.");
                return CommandResult.SUCCESS;
            }
            else
            {
                appliesTo = CheatTarget.PLAYER;
            }
        }

        // If argument is entered, try to parse it as a valid cheat target
        if (appliesTo == null)
        {
            appliesTo = CombatCheatManager.parseTargets(args);
            if (appliesTo == null)
            {
                Console.showMessage("Bad target! Valid targets: " + CollectionUtils.implode(CheatTarget.class) + ".");
                return CommandResult.ERROR;
            }
        }

        CombatCheatManager.enableCheat(CHEAT_ID, "Infinite Ammo (" + appliesTo.name() + ")",
                new InfiniteAmmoPlugin(), appliesTo);
        Console.showMessage("Infinite ammo enabled for " + appliesTo.name() + ".");
        return CommandResult.SUCCESS;
    }

    private static class InfiniteAmmoPlugin extends CheatPlugin
    {

        @Override
        public void advance(@NotNull ShipAPI ship, float amount)
        {
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

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }
    }
}
