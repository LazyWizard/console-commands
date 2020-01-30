package org.lazywizard.console.commands;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatPlugin;
import org.lazywizard.console.cheatmanager.CheatTarget;
import org.lazywizard.console.cheatmanager.CombatCheatManager;
import org.lazywizard.lazylib.CollectionUtils;

public class InfiniteFlux implements BaseCommand
{
    private static final String CHEAT_ID = "lw_console_infiniteflux";

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
                Console.showMessage("Infinite flux disabled.");
                return CommandResult.SUCCESS;
            }
            else
            {
                appliesTo = Console.getSettings().getDefaultCombatCheatTarget();
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

        CombatCheatManager.enableCheat(CHEAT_ID, "Infinite Flux (" + appliesTo.name() + ")",
                new InfiniteFluxPlugin(), appliesTo);
        Console.showMessage("Infinite flux enabled for " + appliesTo.name() + ".");
        return CommandResult.SUCCESS;
    }

    private static class InfiniteFluxPlugin extends CheatPlugin
    {
        @Override
        public void advance(@NotNull ShipAPI ship, float amount)
        {
            if (ship.isHulk() || ship.isShuttlePod())
            {
                return;
            }

            final FluxTrackerAPI flux = ship.getFluxTracker();
            flux.setCurrFlux(0f);
            flux.setHardFlux(0f);

            if (flux.isOverloaded())
            {
                flux.stopOverload();
            }
        }

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }
    }
}
