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
        if (context == CommandContext.CAMPAIGN_MAP)
        {
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            float priorSupplies = player.getCargo().getSupplies();

            for (FleetMemberAPI ship : player.getFleetData().getMembersListCopy())
            {
                RepairTrackerAPI repairs = ship.getRepairTracker();
                // TODO: Calculate actual supplies needed
                float suppliesNeeded = repairs.getMaxRepairCost();
                player.getCargo().addSupplies(suppliesNeeded);
                repairs.performRepairsUsingSupplies(suppliesNeeded);
                repairs.setCR(repairs.getMaxCR());
                ship.setStatUpdateNeeded(true);
            }

            player.getCargo().removeSupplies(player.getCargo().getSupplies() - priorSupplies);
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
        }

        Console.showMessage("Ships repaired.");
        return CommandResult.SUCCESS;
    }
}
