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
import org.lazywizard.lazylib.MathUtils;

public class FlakJacket implements BaseCommand
{
    private static WeakReference plugin = new WeakReference(null);

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        FlakJacketPlugin tmp = (FlakJacketPlugin) plugin.get();
        if (tmp == null || tmp.engine != Global.getCombatEngine())
        {
            tmp = new FlakJacketPlugin();
            plugin = new WeakReference(tmp);
            Global.getCombatEngine().addPlugin(tmp);
            Console.showMessage("Flak jacket enabled.");
        }
        else
        {
            plugin.clear();
            tmp.active = false;
            Console.showMessage("Flak jacket disabled.");
        }

        return CommandResult.SUCCESS;
    }

    private static class FlakJacketPlugin extends BaseEveryFrameCombatPlugin
    {
        private final IntervalUtil nextCheck = new IntervalUtil(0.25f, 0.25f);
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
                ShipAPI player = engine.getPlayerShip();
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
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
        }
    }
}
