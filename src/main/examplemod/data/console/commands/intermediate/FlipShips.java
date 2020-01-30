package data.console.commands.intermediate;

import java.util.Iterator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class FlipShips implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        // This command should only be usable in combat
        if (!context.isInCombat())
        {
            // Show a default error message
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            // Return the 'wrong context' result, this will alert the player by playing a special sound
            return CommandResult.WRONG_CONTEXT;
        }

        // Iterate through all ships on the combat map and flip them 180 degrees
        for (Iterator iter = Global.getCombatEngine().getShips().iterator(); iter.hasNext();)
        {
            ShipAPI toFlip = (ShipAPI) iter.next();
            float newFacing = toFlip.getFacing() + 180f;
            if (newFacing >= 360f)
            {
                newFacing -= 360f;
            }

            toFlip.setFacing(newFacing);
        }

        return CommandResult.SUCCESS;
    }
}
