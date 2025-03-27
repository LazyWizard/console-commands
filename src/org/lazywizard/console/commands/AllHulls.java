package org.lazywizard.console.commands;

import java.util.LinkedHashSet;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AllHulls implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        FleetDataAPI target;
        String targetName;
        int total = 0;

        if (args == null || args.isEmpty())
        {
            target = Storage.retrieveStorageFleetData();
            targetName = "storage (use 'storage' to retrieve)";
        }
        else if ("player".equalsIgnoreCase(args))
        {
            target = Global.getSector().getPlayerFleet().getFleetData();
            targetName = "player fleet";
        }
        else
        {
            SectorEntityToken token = CommandUtils.findTokenInLocation(args,
                    Global.getSector().getCurrentLocation());

            if (token == null)
            {
                Console.showMessage(args + " not found!");
                return CommandResult.ERROR;
            }

            if (token instanceof FleetMemberAPI)
            {
                target = ((FleetMemberAPI) token).getFleetData();
            }
            else
            {
                CargoAPI cargo = CommandUtils.getUsableCargo(token);
                if (cargo.getMothballedShips() == null)
                {
                    cargo.initMothballedShips(token.getFaction().getId());
                }

                target = cargo.getMothballedShips();
            }

            targetName = token.getFullName();
        }

        final Set<String> ids = new LinkedHashSet<>(Global.getSector().getAllEmptyVariantIds());
        for (FleetMemberAPI tmp : target.getMembersListCopy())
        {
            if (!tmp.isFighterWing() && tmp.getVariant().isEmptyHullVariant())
            {
                ids.remove(tmp.getVariant().getHullVariantId());
            }
        }

        for (String id : ids)
        {
            FleetMemberAPI tmp = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, id);
            tmp.getRepairTracker().setMothballed(true);
            target.addFleetMember(tmp);
            total++;
        }

        Console.showMessage("Added " + total + " ships to " + targetName + ".");
        return CommandResult.SUCCESS;
    }
}
