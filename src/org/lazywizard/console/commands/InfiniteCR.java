package org.lazywizard.console.commands;

import com.fs.starfarer.api.combat.ShipAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatPlugin;
import org.lazywizard.console.cheatmanager.CheatTarget;
import org.lazywizard.console.cheatmanager.CombatCheatManager;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class InfiniteCR implements BaseCommandWithSuggestion
{
    private static final String CHEAT_ID = "lw_console_infinitecr";

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
                Console.showMessage("Infinite CR disabled.");
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

        CombatCheatManager.enableCheat(CHEAT_ID, "Infinite CR (" + appliesTo.name() + ")",
                new InfiniteCRPlugin(), appliesTo);
        Console.showMessage("Infinite CR enabled for " + appliesTo.name() + ".");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        List<String> suggestions = new ArrayList<>();
        if (parameter != 0) return suggestions;

        suggestions.add("PLAYER");
        suggestions.add("FLEET");
        suggestions.add("ENEMY");
        suggestions.add("ALL");

        return suggestions;
    }

    private static class InfiniteCRPlugin extends CheatPlugin
    {
        @Override
        public void advance(@NotNull ShipAPI ship, float amount)
        {
            if (ship.losesCRDuringCombat())
            {
                ship.setCurrentCR(Math.max(ship.getCurrentCR(), ship.getCRAtDeployment()));
                ship.getMutableStats().getPeakCRDuration().modifyFlat(
                        "lw_console", ship.getTimeDeployedForCRReduction()
                                * ship.getMutableStats().getCRLossPerSecondPercent().getBonusMult());
            }
        }

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }
    }
}
