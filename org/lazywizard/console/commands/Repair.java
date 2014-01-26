package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class Repair implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.CAMPAIGN)
        {
            Console.showMessage("Error: campaign repair not implemented yet!");
            return CommandResult.WRONG_CONTEXT;
        }

        ArmorGridAPI grid;
        float[][] tmp;
        float sizeX, sizeY;
        float maxArmor;
        for (ShipAPI ship : Global.getCombatEngine().getShips())
        {
            if (ship.getOwner() == FleetSide.PLAYER.ordinal())
            {
                ship.setHitpoints(ship.getMaxHitpoints());

                grid = ship.getArmorGrid();
                maxArmor = grid.getMaxArmorInCell();
                tmp = grid.getGrid();
                sizeX = tmp.length;
                sizeY = tmp[0].length;
                for (int x = 0; x < sizeX; x++)
                {
                    for (int y = 0; y < sizeY; y++)
                    {
                        grid.setArmorValue(x, y, maxArmor);
                    }
                }
            }
        }
        return CommandResult.SUCCESS;
    }
}
