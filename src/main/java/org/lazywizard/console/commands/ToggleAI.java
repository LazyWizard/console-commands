package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatTarget;
import org.lazywizard.console.cheatmanager.CombatCheatManager;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ToggleAI implements BaseCommand
{
    private static final String CDATA_ID = CommonStrings.MOD_ID + "_toggleai_ais";

    @SuppressWarnings("unchecked")
    private static Map<ShipAPI, ShipAIPlugin> getAIMap()
    {
        Map<ShipAPI, ShipAIPlugin> aiMap = (Map<ShipAPI, ShipAIPlugin>) Global.getCombatEngine().getCustomData().get(CDATA_ID);
        if (aiMap == null)
        {
            aiMap = new HashMap<>();
            Global.getCombatEngine().getCustomData().put(CDATA_ID, aiMap);
        }

        return aiMap;
    }

    private static boolean isAIDisabled(ShipAPI target)
    {
        return getAIMap().containsKey(target);
    }

    private static void setAIEnabled(ShipAPI target, boolean enabled)
    {
        final Map<ShipAPI, ShipAIPlugin> aiMap = getAIMap();
        if (enabled && aiMap.containsKey(target))
        {
            final ShipAIPlugin ai = aiMap.remove(target);
            if (ai == null)
            {
                target.resetDefaultAI();
            }
            else
            {
                target.setShipAI(ai);
            }

            target.getShipAI().forceCircumstanceEvaluation();
        }
        else if (!enabled && !aiMap.containsKey(target))
        {
            aiMap.put(target, target.getShipAI());
            target.setShipAI(new NullAI(target));

            for (WeaponGroupAPI group : target.getWeaponGroupsCopy())
            {
                group.toggleOff();
            }
        }
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (!args.isEmpty())
        {
            final String[] tmp = args.split(" ");
            if (tmp.length != 2)
            {
                return CommandResult.BAD_SYNTAX;
            }

            final CheatTarget[] validTargets = {CheatTarget.FLEET, CheatTarget.ENEMY, CheatTarget.ALL};
            final CheatTarget target = CombatCheatManager.parseTargets(tmp[0], validTargets);
            if (target == null)
            {
                Console.showMessage("Bad target! Valid targets: " + CollectionUtils.implode(Arrays.asList(validTargets)) + ".");
                return CommandResult.ERROR;
            }

            final String arg = tmp[1].toLowerCase();
            boolean enabled;
            if ("off".equals(arg) || "false".equals(arg))
            {
                enabled = false;
            }
            else if ("on".equals(arg) || "true".equals(arg))
            {
                enabled = true;
            }
            else
            {
                return CommandResult.BAD_SYNTAX;
            }

            for (ShipAPI ship : Global.getCombatEngine().getShips())
            {
                if (CombatCheatManager.isTarget(ship, target))
                {
                    setAIEnabled(ship, enabled);
                }
            }

            Console.showMessage((enabled ? "Enabled" : "Disabled") + " AI for " + target);
            return CommandResult.SUCCESS;
        }

        final ShipAPI target = Global.getCombatEngine().getPlayerShip().getShipTarget();
        if (target == null)
        {
            Console.showMessage("No target found!");
            return CommandResult.ERROR;
        }

        if (isAIDisabled(target))
        {
            setAIEnabled(target, true);
            Console.showMessage("Re-enabled AI of target");
        }
        else
        {
            setAIEnabled(target, false);
            Console.showMessage("Disabled AI of target");
        }

        return CommandResult.SUCCESS;
    }

    static class NullAI implements ShipAIPlugin
    {
        private final ShipAPI ship;
        private final ShipwideAIFlags flags;

        NullAI(ShipAPI ship)
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
