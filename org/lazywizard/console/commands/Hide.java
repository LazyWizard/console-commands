package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Hide implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Minimize sensor profile
        final String bonusId = CommonStrings.MOD_ID + "_hide";
        final MutableFleetStatsAPI stats = Global.getSector().getPlayerFleet().getStats();
        final StatBonus profileMod = stats.getSensorProfileMod();
        final StatBonus detectMod = stats.getDetectedRangeMod();
        if (profileMod.getMultBonus(bonusId) != null)
        {
            profileMod.unmodify(bonusId);
            detectMod.unmodify(bonusId);
            Console.showMessage("Sensor profile returned to normal.");
            return CommandResult.SUCCESS;
        }

        profileMod.modifyMult(bonusId, 0f, "Console");
        detectMod.modifyMult(bonusId, 0f, "Console");
        Console.showMessage("Sensor profile minimized.");
        return CommandResult.SUCCESS;
    }
}
