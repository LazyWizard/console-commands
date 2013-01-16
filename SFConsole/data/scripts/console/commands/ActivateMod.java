package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import data.scripts.console.BaseCommand;
import java.util.*;

public class ActivateMod extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds a mod to an existing game. The argument is the fully-"
                + "qualified name of the mod's generator's class (for example:"
                + " data.scripts.world.corvus.ModGen).";
    }

    @Override
    protected String getSyntax()
    {
        return "activatemod <fully qualified name of generator>";
    }

    @Override
    protected boolean runCommand(String args)
    {
        args = args.trim();

        if (args.endsWith(".java"))
        {
            args = args.replace("/", ".").replace("\\", ".");
            args = args.substring(0, args.lastIndexOf(".java"));
        }

        if (args.isEmpty() || args.contains(" "))
        {
            showSyntax();
            return false;
        }

        Class genClass;

        try
        {
            genClass = Global.getSettings().getScriptClassLoader().loadClass(args);
        }
        catch (ClassNotFoundException ex)
        {
            showMessage("No generator found with that name! Is the mod activated"
                    + " in the launcher?");
            return false;
        }

        if (!SectorGeneratorPlugin.class.isAssignableFrom(genClass))
        {
            showMessage("Class '" + args + "' does not implement"
                    + " SectorGeneratorPlugin!");
            return false;
        }

        /*int confirm = JOptionPane.showConfirmDialog(null,
         "Are you sure you wish to activate this generator? This action"
         + " will break your game if the mod is not fully compatible"
         + " with all currently activated mods!", "Warning!",
         JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

         if (confirm != JOptionPane.YES_OPTION)
         {
         return false;
         }*/

        SectorGeneratorPlugin generator;

        try
        {
            generator = (SectorGeneratorPlugin) genClass.newInstance();
        }
        catch (InstantiationException ex)
        {
            showError("Error while running generator: ", ex);
            return false;
        }
        catch (IllegalAccessException ex)
        {
            showError("Error while running generator: ", ex);
            return false;
        }
        catch (ClassCastException ex)
        {
            showError("Error while running generator: ", ex);
            return false;
        }

        List<SectorEntityToken> planets = new ArrayList(getStarSystem().getPlanets());
        List<SectorEntityToken> stations = new ArrayList(getStarSystem().getOrbitalStations());
        List<SectorEntityToken> asteroids = new ArrayList(getStarSystem().getAsteroids());
        List<CampaignFleetAPI> fleets = new ArrayList(getStarSystem().getFleets());

        try
        {
            generator.generate(getSector());
        }
        catch (Exception ex)
        {
            showError("Exception while running generator:", ex);
            showMessage("Attempting to revert...");

            List<SectorEntityToken> newPlanets = new ArrayList(getStarSystem().getPlanets());
            newPlanets.removeAll(planets);

            if (!newPlanets.isEmpty())
            {
                showMessage("Removing added planets...");

                for (SectorEntityToken planet : newPlanets)
                {
                    showMessage("DEBUG: Planet " + planet.getFullName());
                    //getStarSystem().removePlanet(planet);
                }
            }

            List<SectorEntityToken> newStations = new ArrayList(getStarSystem().getOrbitalStations());
            newStations.removeAll(stations);

            if (!newStations.isEmpty())
            {
                showMessage("Removing added stations...");

                for (SectorEntityToken station : newStations)
                {
                    showMessage("DEBUG: Station " + station.getFullName());
                    //getStarSystem().removeOrbitalStation(station);
                }
            }

            List<SectorEntityToken> newAsteroids = new ArrayList(getStarSystem().getAsteroids());
            newAsteroids.removeAll(asteroids);

            if (!newAsteroids.isEmpty())
            {
                showMessage("Removing added asteroids...");

                for (SectorEntityToken asteroid : newAsteroids)
                {
                    showMessage("DEBUG: Asteroid " + asteroid.getFullName());
                    //getStarSystem().removeAsteroid(asteroid);
                }
            }

            List<CampaignFleetAPI> newFleets = new ArrayList(getStarSystem().getFleets());
            newFleets.removeAll(fleets);

            if (!newFleets.isEmpty())
            {
                showMessage("Removing added fleets...");

                for (CampaignFleetAPI fleet : newFleets)
                {
                    showMessage("DEBUG: Fleet " + fleet.getFullName());
                    //getStarSystem().removeFleet(fleet);
                }
            }

            showMessage("Reverted successfully.");
            return false;
        }

        showMessage("Generator ran successfully, mod should be active now.");
        return true;
    }
}
