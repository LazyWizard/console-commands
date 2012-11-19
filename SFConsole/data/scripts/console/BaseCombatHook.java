package data.scripts.console;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FogOfWarAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.plugins.BattleObjectivesEffectsPlugin;
import com.fs.starfarer.api.plugins.FogOfWarPlugin;
import java.util.Iterator;

/**
 * Notifies the {@link ConsoleManager} when the game is in battle.
 */
public class BaseCombatHook implements BattleObjectivesEffectsPlugin, FogOfWarPlugin
{
    public static boolean shouldReveal = false, infAmmo = false, noCooldown = false;

    @Override
    public void applyEffects()
    {
        if (Console.getConsole() != null)
        {
            Console.getConsole().checkQueue();

            if (infAmmo || noCooldown)
            {
                ShipAPI tmp;
                WeaponAPI wep;

                for (Iterator allShips = Console.getCombatEngine().getAllShips().iterator(); allShips.hasNext();)
                {
                    tmp = (ShipAPI) allShips.next();

                    if (tmp.getOwner() == FleetSide.PLAYER.ordinal())
                    {
                        for (Iterator allWeps = tmp.getAllWeapons().iterator(); allWeps.hasNext();)
                        {
                            wep = (WeaponAPI) allWeps.next();
                            if (infAmmo)
                            {
                                wep.resetAmmo();
                            }
                            // Bug with burst weapons not reloading - will fix later
                            if (noCooldown && !wep.isBurstBeam())
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

    /**
     * Tells {@link ConsoleManager} the game is in battle and registers the {@link CombatEngineAPI}.<p>
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

        shouldReveal = infAmmo = noCooldown = false;
        Console.setInBattle(true);
        Console.setCombatEngine(engine);
    }

    @Override
    public float getNavBonusPercent(int owner)
    {
        return 0f;
    }

    @Override
    public float getRangeBonusPercent(int owner)
    {
        return 0f;
    }

    @Override
    public void reveal(FogOfWarAPI fogOfWar)
    {
        if (shouldReveal)
        {
            fogOfWar.revealAroundPoint(this, 0, 0, 50000f);
        }
    }

    @Override
    public void hide(FogOfWarAPI fogOfWar)
    {
    }
}