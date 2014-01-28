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
        if (context == CommandContext.CAMPAIGN_MAP)
        {
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            float priorSupplies = player.getCargo().getSupplies();
            float suppliesNeeded;
            RepairTrackerAPI repairs;
            for (FleetMemberAPI ship : player.getFleetData().getMembersListCopy())
            {
                repairs = ship.getRepairTracker();
                // TODO: Calculate actual supplies needed
                suppliesNeeded = repairs.getMaxRepairCost();
                player.getCargo().addSupplies(suppliesNeeded);
                repairs.performRepairsUsingSupplies(suppliesNeeded);
                ship.setStatUpdateNeeded(true);
            }
            //System.out.println("Removed " + (player.getCargo().getSupplies()
            //        - priorSupplies) + " supplies.");
            player.getCargo().removeSupplies(player.getCargo().getSupplies() - priorSupplies);

            Console.showMessage("All ships in fleet repaired.");
            return CommandResult.WRONG_CONTEXT;
        }

        ArmorGridAPI grid;
        float sizeX, sizeY;
        float maxArmor;
        for (ShipAPI ship : Global.getCombatEngine().getShips())
        {
            if (ship.getOwner() == FleetSide.PLAYER.ordinal())
            {
                ship.setHitpoints(ship.getMaxHitpoints());

                grid = ship.getArmorGrid();
                maxArmor = grid.getMaxArmorInCell();
                sizeX = grid.getGrid().length;
                sizeY = grid.getGrid()[0].length;
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
