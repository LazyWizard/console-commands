package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class Repair implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        // Used in campaign: repair all ships in fleet (same as station repair)
        if (context.isInCampaign())
        {
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();

            for (FleetMemberAPI ship : player.getFleetData().getMembersListCopy())
            {
                ship.getStatus().repairFully();
                RepairTrackerAPI repairs = ship.getRepairTracker();
                repairs.setCR(Math.max(repairs.getCR(), repairs.getMaxCR()));
                ship.setStatUpdateNeeded(true);
            }

            Console.showMessage("All ships in fleet repaired.");
            return CommandResult.SUCCESS;
        }

        // Used in combat: repair hull/armor of all friendly ships on the field
        for (ShipAPI ship : Global.getCombatEngine().getShips())
        {
            if (ship.getOwner() == FleetSide.PLAYER.ordinal())
            {
                ship.setHitpoints(ship.getMaxHitpoints());

                ArmorGridAPI grid = ship.getArmorGrid();
                float maxArmor = grid.getMaxArmorInCell();
                float sizeX = grid.getGrid().length;
                float sizeY = grid.getGrid()[0].length;
                for (int x = 0; x < sizeX; x++)
                {
                    for (int y = 0; y < sizeY; y++)
                    {
                        grid.setArmorValue(x, y, maxArmor);
                    }
                }
            }

            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();
        }

        Console.showMessage("Ships repaired.");
        return CommandResult.SUCCESS;
    }
}
