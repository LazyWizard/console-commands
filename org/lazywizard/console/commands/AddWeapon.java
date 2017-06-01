package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddWeapon implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " 1", context);
        }

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        final int amount;
        if (!isInteger(tmp[1]))
        {
            // Support for reversed arguments
            if (!isInteger(tmp[0]))
            {
                return CommandResult.BAD_SYNTAX;
            }

            amount = Integer.parseInt(tmp[0]);
            tmp[0] = tmp[1];
        }
        else
        {
            amount = Integer.parseInt(tmp[1]);
        }

        final String wepId = findBestStringMatch(tmp[0], Global.getSector().getAllWeaponIds());
        if (wepId == null)
        {
            Console.showMessage("No weapon found with id '" + tmp[0]
                    + "'! Use 'list weapons' for a complete list of valid ids.");
            return CommandResult.ERROR;
        }

        final CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (amount >= 0)
        {
            cargo.addWeapons(wepId, amount);
            Console.showMessage("Added " + format(amount) + " of weapon "
                    + wepId + " to player inventory.");
        }
        else
        {
            final int removed = Math.min(-amount, cargo.getNumWeapons(wepId));
            cargo.removeWeapons(wepId, removed);
            Console.showMessage("Removed " + format(removed) + " of weapon "
                    + wepId + " from player inventory.");
        }

        return CommandResult.SUCCESS;
    }
}
