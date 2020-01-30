package data.console.commands.advanced;

import java.lang.ref.WeakReference;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatPlugin;
import org.lazywizard.console.cheatmanager.CombatCheatManager;
import org.lazywizard.lazylib.MathUtils;

public class FlakJacket implements BaseCommand
{
    private static final String CHEAT_ID = "lw_console_flakjacket";

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (CombatCheatManager.isEnabled(CHEAT_ID))
        {
            CombatCheatManager.disableCheat(CHEAT_ID);
            Console.showMessage("Flak jacket disabled.");
            return CommandResult.SUCCESS;
        }
        else
        {
            CombatCheatManager.enableCheat(CHEAT_ID, "Flak Jacket", new FlakJacketPlugin(), null);
            Console.showMessage("Flak jacket enabled.");
            return CommandResult.SUCCESS;
        }
    }

    private static class FlakJacketPlugin extends CheatPlugin
    {
        private final IntervalUtil nextCheck = new IntervalUtil(0.25f, 0.25f);
        private boolean active = true;
        private CombatEngineAPI engine;

        @Override
        public void advance(float amount, List events)
        {
            nextCheck.advance(amount);
            if (nextCheck.intervalElapsed())
            {
                final CombatEngineAPI engine = Global.getCombatEngine();
                final ShipAPI player = engine.getPlayerShip();
                for (int x = 0; x < 360; x += 10)
                {
                    engine.spawnProjectile(player, null, "flak",
                            MathUtils.getPointOnCircumference(
                                    player.getLocation(), player.getCollisionRadius(),
                                    x), x + 90f, player.getVelocity());
                }
            }
        }

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }
    }
}
