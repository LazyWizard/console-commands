package data.console.commands.advanced;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class ForeverAlone implements BaseCommand
{
    private static WeakReference lastSector = new WeakReference(null);

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Check if the command is active on this savegame
        SectorAPI tmp = (SectorAPI) lastSector.get();
        if (tmp != null && tmp == Global.getSector())
        {
            Global.getSector().removeScriptsOfClass(ForeverAloneScript.class);
            lastSector.clear();
            Console.showMessage("I have friends!");
            return CommandResult.SUCCESS;
        }

        // If not already active, add script and register that it's active
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        Global.getSector().addTransientScript(new ForeverAloneScript(player));
        lastSector = new WeakReference(Global.getSector());
        Console.showMessage("Why doesn't anyone love me?");
        return CommandResult.SUCCESS;
    }

    private static class ForeverAloneScript implements EveryFrameScript
    {
        private static final float MINIMUM_DISTANCE = 20f;
        private final CampaignFleetAPI fleet;

        private ForeverAloneScript(CampaignFleetAPI fleet)
        {
            this.fleet = fleet;
        }

        @Override
        public boolean isDone()
        {
            return false;
        }

        @Override
        public boolean runWhilePaused()
        {
            return true;
        }

        @Override
        public void advance(float amount)
        {
            LocationAPI loc = fleet.getContainingLocation();
            if (loc == null || !fleet.isAlive())
            {
                return;
            }

            // Constantly check for nearby fleets, move them back and set facing away from player
            for (Iterator iter = loc.getFleets().iterator(); iter.hasNext();)
            {
                CampaignFleetAPI other = (CampaignFleetAPI) iter.next();

                if (other == fleet)
                {
                    continue;
                }

                if (MathUtils.isWithinRange(fleet, other, MINIMUM_DISTANCE))
                {
                    float newFacing = VectorUtils.getAngle(
                            fleet.getLocation(), other.getLocation());
                    Vector2f newLocation = MathUtils.getPointOnCircumference(
                            fleet.getLocation(), MINIMUM_DISTANCE
                            + fleet.getRadius() + other.getRadius(), newFacing);
                    other.setLocation(newLocation.x, newLocation.y);
                }
            }
        }
    }
}
