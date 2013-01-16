package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.mission.FleetSide;
import java.util.*;

/**
 * Notifies the {@link Console} when the game is in battle.
 */
public class BaseCombatHook implements EveryFrameCombatPlugin
{
    private static final String CONSOLE_ID = "consolemod";
    private static final boolean RESET_COMMANDS_AFTER_COMBAT = false;
    private static boolean showActive = true, shouldReveal = false, infAmmo = false,
            infFlux = false, godMode = false, noCooldown = false, nuke = false;
    private static CombatEngineAPI engine;

    public static boolean toggleGodMode()
    {
        godMode = !godMode;
        showActive = true;
        return godMode;
    }

    public static boolean toggleInfiniteAmmo()
    {
        infAmmo = !infAmmo;
        showActive = true;
        return infAmmo;
    }

    public static boolean toggleInfiniteFlux()
    {
        infFlux = !infFlux;
        showActive = true;
        return infFlux;
    }

    public static boolean toggleNoCooldown()
    {
        noCooldown = !noCooldown;
        showActive = true;
        return noCooldown;
    }

    public static boolean toggleReveal()
    {
        shouldReveal = !shouldReveal;
        showActive = true;
        return shouldReveal;
    }

    public static boolean enableNuke()
    {
        nuke = true;
        Console.showMessage("Nuke activated. All enemy ships destroyed.");
        return true;
    }

    private void checkRender()
    {
        if (showActive && engine.getPlayerShip() != null)
        {
            SortedSet<String> active = new TreeSet();

            if (godMode)
            {
                active.add("God");
            }
            if (infAmmo)
            {
                active.add("InfiniteAmmo");
            }
            if (infFlux)
            {
                active.add("InfiniteFlux");
            }
            if (noCooldown)
            {
                active.add("NoCooldown");
            }
            if (shouldReveal)
            {
                active.add("Reveal");
            }

            if (!active.isEmpty())
            {
                StringBuilder msg = new StringBuilder("Active commands: ");

                for (String str : active)
                {
                    if (str != active.first())
                    {
                        msg.append(", ");
                    }

                    msg.append(str);
                }

                Console.showMessage(msg.toString());
            }

            showActive = false;
        }
    }

    private void checkCommands()
    {
        if (godMode || infFlux || infAmmo || noCooldown || nuke)
        {
            for (ShipAPI ship : engine.getShips())
            {
                if (ship.isHulk() || ship.isShuttlePod())
                {
                    continue;
                }

                if (ship.getOwner() == FleetSide.PLAYER.ordinal())
                {
                    if (godMode)
                    {
                        ship.getMutableStats().getHullDamageTakenMult().modifyPercent(CONSOLE_ID, -1000f);
                        ship.getMutableStats().getEmpDamageTakenMult().modifyPercent(CONSOLE_ID, -1000f);
                    }
                    else
                    {
                        ship.getMutableStats().getHullDamageTakenMult().unmodify(CONSOLE_ID);
                        ship.getMutableStats().getEmpDamageTakenMult().unmodify(CONSOLE_ID);
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
                else
                {
                    if (nuke)
                    {
                        engine.applyDamage(ship, ship.getLocation(), 500000,
                                DamageType.ENERGY, 500000, true, false, null);
                    }
                }
            }
        }

        if (shouldReveal)
        {
            engine.getFogOfWar(FleetSide.PLAYER.ordinal()).
                    revealAroundPoint(this, 0, 0,
                    Math.max(engine.getMapHeight(), engine.getMapWidth()));
        }

        nuke = false;
    }

    @Override
    public void advance(float amount, List events)
    {
        if (Console.getConsole() != null)
        {
            Console.getConsole().checkQueue();

            if (!engine.isPaused())
            {
                checkCommands();
                checkRender();
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
        BaseCombatHook.engine = engine;

        if (Console.getConsole() == null)
        {
            Console.setConsole(new Console());
        }

        showActive = true;
        nuke = false;

        if (RESET_COMMANDS_AFTER_COMBAT)
        {
            shouldReveal = infAmmo = infFlux = godMode = noCooldown = false;
        }

        Console.setInBattle(true);
        Console.setCombatEngine(engine);
    }
}