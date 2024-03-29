package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.*;

import static org.lazywizard.console.CommandUtils.findBestFactionMatch;

// TODO: This needs cleanup after adding the optional faction argument (supporting logic is ugly)
// TODO: Add resetblueprints command
public class AllBlueprints implements BaseCommand
{
    public static boolean isLearnable(ShipHullSpecAPI spec)
    {
        if (spec.getHullSize() == HullSize.FIGHTER) return false;

        for (String tag : spec.getTags())
        {
            if (tag.endsWith("_bp")) return true;
        }

        return false;
    }

    public static boolean isLearnable(FighterWingSpecAPI spec)
    {
        for (String tag : spec.getTags())
        {
            if (tag.endsWith("_bp")) return true;
        }

        return false;
    }

    public static boolean isLearnable(WeaponSpecAPI spec)
    {
        for (String tag : spec.getTags())
        {
            if (tag.endsWith("_bp")) return true;
        }

        return false;
    }

    public static boolean isLearnable(IndustrySpecAPI spec)
    {
        // TODO: filter out industries that are known by default
        return true;
    }

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
            args = "all";
        }

        final Set<FactionAPI> factions = new HashSet<>();
        boolean unlockShips = false, unlockWings = false,
                unlockWeapons = false, unlockIndustries = false;
        for (String arg : args.split(" "))
        {
            switch (arg.toLowerCase())
            {
                case "ship":
                case "ships":
                    unlockShips = true;
                    break;
                case "wing":
                case "wings":
                case "fighter":
                case "fighters":
                case "lpc":
                case "lpcs":
                    unlockWings = true;
                    break;
                case "weapon":
                case "weapons":
                    unlockWeapons = true;
                    break;
                case "industry":
                case "industries":
                    unlockIndustries = true;
                    break;
                case "all":
                    unlockShips = unlockWings = unlockWeapons /*= unlockIndustries*/ = true;
                    break;
                default:
                    final FactionAPI faction = findBestFactionMatch(arg);
                    if (faction != null)
                    {
                        factions.add(faction);
                    }
                    else
                    {
                        return CommandResult.BAD_SYNTAX;
                    }
            }
        }

        // No arguments defaults to all, but isn't detected if you passed in a list of factions
        if (!unlockShips && !unlockWings && !unlockWeapons && !unlockIndustries && !factions.isEmpty())
        {
            unlockShips = unlockWings = unlockWeapons /*= unlockIndustries*/ = true;
        }

        if (!factions.isEmpty())
        {
            final List<String> names = new ArrayList<>(factions.size());
            for (FactionAPI faction : factions)
            {
                names.add(faction.getDisplayName());
            }

            Console.showMessage("Limiting to blueprints from faction" + (factions.size() > 1 ? "s " : " ")
                    + CollectionUtils.implode(names) + ".");
        }

        final FactionAPI player = Global.getSector().getPlayerFaction();
        if (unlockShips)
        {
            final List<String> unlocked = new ArrayList<>();
            for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs())
            {
                if (isLearnable(spec) && !player.knowsShip(spec.getHullId()))
                {
                    if (factions.isEmpty())
                    {
                        player.addKnownShip(spec.getHullId(), true);
                        unlocked.add(spec.getHullId() + " (" + spec.getHullNameWithDashClass() + ")");
                    }
                    else
                    {
                        for (FactionAPI faction : factions)
                        {
                            if (faction.knowsShip(spec.getHullId()))
                            {
                                player.addKnownShip(spec.getHullId(), true);
                                unlocked.add(spec.getHullId() + " (" + spec.getHullNameWithDashClass() + ")");
                            }
                        }
                    }
                }
            }

            if (unlocked.isEmpty())
            {
                Console.showMessage("You already know all unlockable ships!");
            }
            else
            {
                Collections.sort(unlocked, String.CASE_INSENSITIVE_ORDER);
                Console.showIndentedMessage("Unlocked " + unlocked.size() + " ships:",
                        CollectionUtils.implode(unlocked), 3);
            }
        }

        if (unlockWings)
        {
            final List<String> unlocked = new ArrayList<>();
            for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs())
            {
                if (isLearnable(spec) && !player.knowsFighter(spec.getId()))
                {
                    if (factions.isEmpty())
                    {
                        player.addKnownFighter(spec.getId(), true);
                        unlocked.add(spec.getId() + " (" + spec.getWingName() + ")");
                    }
                    else
                    {
                        for (FactionAPI faction : factions)
                        {
                            if (faction.knowsFighter(spec.getId()))
                            {
                                player.addKnownFighter(spec.getId(), true);
                                unlocked.add(spec.getId() + " (" + spec.getWingName() + ")");
                            }
                        }
                    }
                }
            }

            if (unlocked.isEmpty())
            {
                Console.showMessage("You already know all unlockable fighter wings!");
            }
            else
            {
                Collections.sort(unlocked, String.CASE_INSENSITIVE_ORDER);
                Console.showIndentedMessage("Unlocked " + unlocked.size() + " fighter wings:",
                        CollectionUtils.implode(unlocked), 3);
            }
        }

        if (unlockWeapons)
        {
            final List<String> unlocked = new ArrayList<>();
            for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs())
            {
                if (isLearnable(spec) && !player.knowsWeapon(spec.getWeaponId()))
                {
                    if (factions.isEmpty())
                    {
                        player.addKnownWeapon(spec.getWeaponId(), true);
                        unlocked.add(spec.getWeaponId() + " (" + spec.getWeaponName() + ")");
                    }
                    else
                    {
                        for (FactionAPI faction : factions)
                        {
                            if (faction.knowsWeapon(spec.getWeaponId()))
                            {
                                player.addKnownWeapon(spec.getWeaponId(), true);
                                unlocked.add(spec.getWeaponId() + " (" + spec.getWeaponName() + ")");
                            }
                        }
                    }
                }
            }

            if (unlocked.isEmpty())
            {
                Console.showMessage("You already know all unlockable weapons!");
            }
            else
            {
                Collections.sort(unlocked, String.CASE_INSENSITIVE_ORDER);
                Console.showIndentedMessage("Unlocked " + unlocked.size() + " weapons:",
                        CollectionUtils.implode(unlocked), 3);
            }
        }

        if (unlockIndustries)
        {
            final List<String> unlocked = new ArrayList<>();
            for (IndustrySpecAPI spec : Global.getSettings().getAllIndustrySpecs())
            {
                if (isLearnable(spec) && !player.knowsIndustry(spec.getId()))
                {
                    if (factions.isEmpty())
                    {
                        player.addKnownIndustry(spec.getId());
                        unlocked.add(spec.getId() + " (" + spec.getName() + ")");
                    }
                    else
                    {
                        for (FactionAPI faction : factions)
                        {
                            if (faction.knowsIndustry(spec.getId()))
                            {
                                player.addKnownIndustry(spec.getId());
                                unlocked.add(spec.getId() + " (" + spec.getName() + ")");
                            }
                        }
                    }
                }
            }

            if (unlocked.isEmpty())
            {
                Console.showMessage("You already know all unlockable industries!");
            }
            else
            {
                Collections.sort(unlocked, String.CASE_INSENSITIVE_ORDER);
                Console.showIndentedMessage("Unlocked " + unlocked.size() + " industries:",
                        CollectionUtils.implode(unlocked), 3);
            }
        }

        return CommandResult.SUCCESS;
    }
}
