package org.lazywizard.console.commands;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatPlugin;
import org.lazywizard.console.cheatmanager.CheatTarget;
import org.lazywizard.console.cheatmanager.CombatCheatManager;
import org.lazywizard.lazylib.CollectionUtils;

public class NoCooldown implements BaseCommand
{
    private static final String CHEAT_ID = "lw_console_nocooldown";

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
                Console.showMessage("Cooldowns enabled.");
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

        CombatCheatManager.enableCheat(CHEAT_ID, "No Cooldowns (" + appliesTo.name() + ")",
                new NoCooldownPlugin(), appliesTo);
        Console.showMessage("Cooldowns disabled for " + appliesTo.name() + ".");
        return CommandResult.SUCCESS;
    }

    private static class NoCooldownPlugin extends CheatPlugin
    {
        @Override
        public void advance(@NotNull ShipAPI ship, float amount)
        {
            for (WeaponAPI wep : ship.getAllWeapons())
            {
                if (wep.getCooldownRemaining() > 0f)
                {
                    wep.setRemainingCooldownTo(0.0001f);
                }
            }

            final ShipSystemAPI system = ship.getSystem();
            if (system != null && system.isCoolingDown()) system.setCooldownRemaining(0.0001f);
        }

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }
    }
}
