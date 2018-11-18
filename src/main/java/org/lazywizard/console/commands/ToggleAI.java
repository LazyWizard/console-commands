package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Map;
import java.util.WeakHashMap;

public class ToggleAI implements BaseCommand
{
    private static final Map<ShipAPI, ShipAIPlugin> ais = new WeakHashMap<>();

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        ShipAPI target = Global.getCombatEngine().getPlayerShip().getShipTarget();
        if (target == null)
        {
            Console.showMessage("No target found!");
            return CommandResult.ERROR;
        }

        if (ais.containsKey(target))
        {
            ShipAIPlugin ai = ais.remove(target);
            if (ai == null)
            {
                target.resetDefaultAI();
            }
            else
            {
                target.setShipAI(ai);
            }

            target.getShipAI().forceCircumstanceEvaluation();
            Console.showMessage("Re-enabled AI of target");
        }
        else
        {
            ais.put(target, target.getShipAI());
            target.setShipAI(new NullAI(target));

            for (WeaponGroupAPI group : target.getWeaponGroupsCopy())
            {
                group.toggleOff();
            }

            Console.showMessage("Disabled AI of target");
        }

        return CommandResult.SUCCESS;
    }

    private static class NullAI implements ShipAIPlugin
    {
        private final ShipAPI ship;
        private final ShipwideAIFlags flags;

        private NullAI(ShipAPI ship)
        {
            this.ship = ship;
            flags = new ShipwideAIFlags();
        }

        @Override
        public void setDoNotFireDelay(float amount)
        {
        }

        @Override
        public void forceCircumstanceEvaluation()
        {
        }

        @Override
        public void advance(float amount)
        {
            if (ship.getVelocity().lengthSquared() > 0f)
            {
                ship.giveCommand(ShipCommand.DECELERATE, null, 0);
            }
        }

        @Override
        public boolean needsRefit()
        {
            return false;
        }

        @Override
        public ShipwideAIFlags getAIFlags()
        {
            return flags;
        }

        @Override
        public void cancelCurrentManeuver()
        {
        }

        @Override
        public ShipAIConfig getConfig()
        {
            return null;
        }
    }
}
