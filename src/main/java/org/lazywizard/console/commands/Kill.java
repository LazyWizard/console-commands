package org.lazywizard.console.commands;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

public class Kill implements BaseCommand
{
    public static void killShip(ShipAPI target, boolean creditKillToPlayer)
    {
        if (target == null)
        {
            return;
        }

        // Ensure we hit (needed for certain oddly-shaped mod ships)
        Vector2f hitLoc = target.getLocation();
        if (!CollisionUtils.isPointWithinBounds(hitLoc, target))
        {
            if (!target.getAllWeapons().isEmpty())
            {
                //System.out.println("Using alternate hit location for "
                //        + ship.getHullSpec().getHullId());
                hitLoc = target.getAllWeapons().get(0).getLocation();
            }
            else if (!target.getEngineController().getShipEngines().isEmpty())
            {
                hitLoc = target.getEngineController().getShipEngines().get(0).getLocation();
            }
            else
            {
                Console.showMessage("Couldn't kill " + target.getHullSpec().getHullId());
            }
        }

        // Ensure a kill
        target.getMutableStats().getHullDamageTakenMult().unmodify();
        target.getMutableStats().getArmorDamageTakenMult().unmodify();
        target.setHitpoints(1f);
        int[] cell = target.getArmorGrid().getCellAtLocation(hitLoc);
        target.getArmorGrid().setArmorValue(cell[0], cell[1], 0f);
        Global.getCombatEngine().applyDamage(target, hitLoc, 500_000,
                DamageType.OTHER, 500_000, true, false, (creditKillToPlayer
                        ? Global.getCombatEngine().getPlayerShip() : null));
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context.isInCampaign())
        {
            Global.getSector().removeScriptsOfClass(KillOnClickScript.class);
            Global.getSector().addTransientScript(new KillOnClickScript());
            Console.showMessage("Click on fleets to destroy them. Press escape when you are finished.");
            return CommandResult.SUCCESS;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI target = engine.getPlayerShip().getShipTarget();

        if (target == null)
        {
            Console.showMessage("No target found!");
            return CommandResult.ERROR;
        }

        killShip(target, true);
        Console.showMessage("Destroyed " + target.getVariant().getFullDesignationWithHullName() + ".");
        return CommandResult.SUCCESS;
    }

    private static class KillOnClickScript implements EveryFrameScript
    {
        private static final float TIME_BETWEEN_NOTIFICATIONS = 15f;
        private float nextNotify = 0f, timeUntilEscapeRegisters = 1f;
        private boolean buttonDown = false, isDone = false;

        @Override
        public boolean isDone()
        {
            return isDone;
        }

        @Override
        public boolean runWhilePaused()
        {
            return true;
        }

        @Override
        public void advance(float amount)
        {
            final CampaignUIAPI ui = Global.getSector().getCampaignUI();
            if (isDone || ui.isShowingDialog() || ui.isShowingMenu())
            {
                return;
            }

            timeUntilEscapeRegisters -= amount;
            if (timeUntilEscapeRegisters <= 0f && Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
            {
                isDone = true;
                ui.addMessage("Cancelled.", Console.getSettings().getOutputColor());
                return;
            }

            if (Mouse.isButtonDown(0))
            {
                if (!buttonDown)
                {
                    buttonDown = true;
                    nextNotify = TIME_BETWEEN_NOTIFICATIONS;
                    final LocationAPI loc = Global.getSector().getCurrentLocation();
                    final ViewportAPI view = Global.getSector().getViewport();
                    final Vector2f target = new Vector2f(view.convertScreenXToWorldX(Global.getSettings().getMouseX()),
                            view.convertScreenYToWorldY(Global.getSettings().getMouseY()));
                    for (CampaignFleetAPI fleet : loc.getFleets())
                    {
                        if (fleet.isPlayerFleet())
                        {
                            continue;
                        }

                        if (MathUtils.isWithinRange(target, fleet.getLocation(),
                                fleet.getRadius()))
                        {
                            ui.addMessage("Destroyed " + fleet.getFullName()
                                    + " (" + fleet.getFleetPoints() + " FP).",
                                    Console.getSettings().getOutputColor());
                            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy())
                            {
                                fleet.removeFleetMemberWithDestructionFlash(member);
                            }
                        }
                    }
                }
            }
            else
            {
                buttonDown = false;
                nextNotify -= amount;
                if (nextNotify <= 0f)
                {
                    ui.addMessage("Click on fleets to destroy them, or press"
                            + " escape to cancel...", Console.getSettings().getOutputColor());
                    nextNotify = TIME_BETWEEN_NOTIFICATIONS;
                }
            }
        }
    }
}
