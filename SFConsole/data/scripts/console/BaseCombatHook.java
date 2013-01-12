package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.mission.FleetSide;
import java.util.List;

/**
 * Notifies the {@link Console} when the game is in battle.
 */
public class BaseCombatHook implements EveryFrameCombatPlugin
{
    private static final String CONSOLE_ID = "consolemod";
    private static final boolean RESET_COMMANDS_AFTER_COMBAT = false;
    public static boolean shouldReveal = false, infAmmo = false,
            infFlux = false, godMode = false, noCooldown = false;

    @Override
    public void advance(float amount, List events)
    {
        if (Console.getConsole() != null)
        {
            Console.getConsole().checkQueue();

            if (shouldReveal)
            {
                Console.getCombatEngine().getFogOfWar(FleetSide.PLAYER.ordinal()).
                        revealAroundPoint(this, 0, 0, 50000f);
            }

            if (godMode || infFlux || infAmmo || noCooldown)
            {
                for (ShipAPI ship : Console.getCombatEngine().getShips())
                {
                    if (ship.isHulk() || ship.isShuttlePod())
                    {
                        continue;
                    }

                    if (ship.getOwner() == FleetSide.PLAYER.ordinal())
                    {
                        if (godMode)
                        {
                            ship.getMutableStats().getArmorDamageTakenMult().modifyFlat(CONSOLE_ID, -10000f);
                            ship.getMutableStats().getHullDamageTakenMult().modifyFlat(CONSOLE_ID, -10000f);
                            ship.getMutableStats().getMaxHullRepairFraction().modifyFlat(CONSOLE_ID, 100f);
                            ship.getMutableStats().getHullRepairRatePercentPerSecond().modifyFlat(CONSOLE_ID, 10000f);
                        }
                        else
                        {
                            ship.getMutableStats().getArmorDamageTakenMult().unmodify(CONSOLE_ID);
                            ship.getMutableStats().getHullDamageTakenMult().unmodify(CONSOLE_ID);
                            ship.getMutableStats().getMaxHullRepairFraction().unmodify(CONSOLE_ID);
                            ship.getMutableStats().getHullRepairRatePercentPerSecond().unmodify(CONSOLE_ID);
                        }

                        if (infFlux)
                        {
                            ship.getFluxTracker().setCurrFlux(0f);
                            ship.getFluxTracker().setHardFlux(0f);
                        }

                        if (infAmmo || noCooldown)
                        {
                            for (WeaponAPI wep : ship.getAllWeapons())
                            {
                                if (infAmmo)
                                {
                                    wep.resetAmmo();
                                }
                                if (noCooldown)
                                {
                                    wep.setRemainingCooldownTo(Math.min(.1f,
                                            wep.getCooldownRemaining()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Tells {@link Console} the game is in battle and registers the {@link CombatEngineAPI}.<p>
     *
     * Called by the game automatically at the start of each battle.
     *
     * @param engine the {@link CombatEngineAPI} the current battle uses
     */
    @Override
    public void init(CombatEngineAPI engine)
    {
        if (Console.getConsole() == null)
        {
            Console.setConsole(new Console());
        }

        if (RESET_COMMANDS_AFTER_COMBAT)
        {
            shouldReveal = infAmmo = infFlux = godMode = noCooldown = false;
        }

        Console.setInBattle(true);
        Console.setCombatEngine(engine);
    }
}