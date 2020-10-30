package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.console.commands.List_;

import java.text.NumberFormat;
import java.util.*;

// TODO: Javadoc this class
public class CommandUtils
{
    private static final boolean ENABLE_TYPO_CORRECTION = true;
    private static final String COMBAT_PDATA_ID = "lw_console_plugins";

    /**
     * Returns normalized score, with 0.0 meaning no similarity at all,
     * and 1.0 meaning full equality.
     */
    // Taken from: https://github.com/larsga/Duke/blob/master/duke-core/src/main/java/no/priv/garshol/duke/comparators/JaroWinkler.java
    private static double calcSimilarity(String s1, String s2)
    {
        if (s1.equals(s2))
        {
            return 1.0;
        }

        // ensure that s1 is shorter than or same length as s2
        if (s1.length() > s2.length())
        {
            String tmp = s2;
            s2 = s1;
            s1 = tmp;
        }

        // (1) find the number of characters the two strings have in common.
        // note that matching characters can only be half the length of the
        // longer string apart.
        int maxdist = s2.length() / 2;
        int c = 0; // count of common characters
        int t = 0; // count of transpositions
        int prevpos = -1;
        for (int ix = 0; ix < s1.length(); ix++)
        {
            char ch = s1.charAt(ix);

            // now try to find it in s2
            for (int ix2 = Math.max(0, ix - maxdist);
                 ix2 < Math.min(s2.length(), ix + maxdist);
                 ix2++)
            {
                if (ch == s2.charAt(ix2))
                {
                    c++; // we found a common character
                    if (prevpos != -1 && ix2 < prevpos)
                    {
                        t++; // moved back before earlier
                    }
                    prevpos = ix2;
                    break;
                }
            }
        }

        // we don't divide t by 2 because as far as we can tell, the above
        // code counts transpositions directly.
        // System.out.println("c: " + c);
        // System.out.println("t: " + t);
        // System.out.println("c/m: " + (c / (double) s1.length()));
        // System.out.println("c/n: " + (c / (double) s2.length()));
        // System.out.println("(c-t)/c: " + ((c - t) / (double) c));
        // we might have to give up right here
        if (c == 0)
        {
            return 0.0;
        }

        // first compute the score
        double score = ((c / (double) s1.length())
                + (c / (double) s2.length())
                + ((c - t) / (double) c)) / 3.0;

        // (2) common prefix modification
        int p = 0; // length of prefix
        int last = Math.min(4, s1.length());
        while (p < last && s1.charAt(p) == s2.charAt(p))
        {
            p++;
        }

        score += ((p * (1 - score)) / 10);

        // (3) longer string adjustment
        // I'm confused about this part. Winkler's original source code includes
        // it, and Yancey's 2005 paper describes it. However, Winkler's list of
        // test cases in his 2006 paper does not include this modification. So
        // is this part of Jaro-Winkler, or is it not? Hard to say.
        if (s1.length() >= 5 // both strings at least 5 characters long
                && c - p >= 2// at least two common characters besides prefix
                && c - p >= ((s1.length() - p) / 2)) // fairly rich in common chars
        {
            score = score + ((1 - score) * ((c - (p + 1))
                    / ((double) ((s1.length() + s2.length())
                    - (2 * (p - 1))))));
        }

        // (4) similar characters adjustment
        // the same holds for this as for (3) above.
        return score;
    }

    public static String findBestStringMatch(String id, Collection<String> toSearch)
    {
        if (toSearch.contains(id))
        {
            return id;
        }

        id = id.toLowerCase();
        String bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        for (String str : toSearch)
        {
            double distance = calcSimilarity(id, str.toLowerCase());

            if (distance == 1.0)
            {
                return str;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = str;
            }
        }

        return bestMatch;
    }

    /**
     * @return A {@link Map.Entry} whose key contains the closest match to {@code id} (or null if no match was found),
     *         and whose value contains the {@link Collection} the match came from, if one was found.
     */
    public static Map.Entry<String, Collection<String>> findBestStringMatch(String id, Collection<String>... sources)
    {
        Collection<String> bestSource = null;
        String bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();
        for (Collection<String> toSearch : sources)
        {
            if (toSearch.contains(id))
            {
                return new AbstractMap.SimpleImmutableEntry<>(id, toSearch);
            }

            id = id.toLowerCase();

            for (String str : toSearch)
            {
                double distance = calcSimilarity(id, str.toLowerCase());

                if (distance == 1.0)
                {
                    return new AbstractMap.SimpleImmutableEntry<>(id, toSearch);
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = str;
                    bestSource = toSearch;
                }
            }
        }

        return new AbstractMap.SimpleImmutableEntry<>(bestMatch, bestSource);
    }

    public static Map.Entry<String, Double> findBestStringMatchWithSimularity(String id, Collection<String> toSearch)
    {
        if (toSearch.contains(id))
        {
            return new AbstractMap.SimpleImmutableEntry<>(id, 1.0);
        }

        id = id.toLowerCase();
        String bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        for (String str : toSearch)
        {
            double distance = calcSimilarity(id, str.toLowerCase());

            if (distance == 1.0)
            {
                return new AbstractMap.SimpleImmutableEntry<>(str, 1.0);
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = str;
            }
        }

        return new AbstractMap.SimpleImmutableEntry<>(bestMatch, closestDistance);
    }

    public static String bestMatch(List<Map.Entry<String, Float>> toSearch)
    {
        String bestMatch = null;
        float bestScore = 0f;
        for (Map.Entry<String, Float> entry : toSearch)
        {
            final String id = entry.getKey();
            final float score = entry.getValue();
            if (id != null)
            {
                if (bestMatch == null || score > bestScore)
                {
                    bestMatch = id;
                    bestScore = score;
                }
            }
        }

        return bestMatch;
    }

    public static boolean isInteger(String arg)
    {
        if (arg.isEmpty())
        {
            return false;
        }

        try
        {
            Integer.parseInt(arg);
            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }

    public static boolean isLong(String arg)
    {
        if (arg.isEmpty())
        {
            return false;
        }

        try
        {
            Long.parseLong(arg);
            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }

    public static boolean isFloat(String arg)
    {
        if (arg.isEmpty())
        {
            return false;
        }

        try
        {
            Float.parseFloat(arg);
            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }

    public static boolean isDouble(String arg)
    {
        if (arg.isEmpty())
        {
            return false;
        }

        try
        {
            Double.parseDouble(arg);
            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }

    public static String format(int toFormat)
    {
        return NumberFormat.getNumberInstance().format(toFormat);
    }

    public static String format(float toFormat)
    {
        return NumberFormat.getNumberInstance().format(toFormat);
    }

    /**
     * Indents and word-wraps a {@link String} to fit within the console's overlay.
     *
     * @param message             The {@link String} to be indented.
     * @param indentation         The number of spaces to indent {@code message} with.
     * @param existingIndentation Any existing indentation, for complicated messages that are indented multiple times.
     *                            Can be {@code null}.
     *
     * @return {@code message}, indented and word-wrapped to fit within the console overlay.
     *
     * @since 3.0
     */
    public static String indent(String message, int indentation, @Nullable String existingIndentation)
    {
        if (existingIndentation != null)
        {
            final float maxWidth = Console.getScrollbackWidth() - (Console.getFont().calcWidth(
                    existingIndentation, Console.getFontSize()));
            return Console.getFont().wrapString(message, Console.getFontSize(),
                    maxWidth, Float.MAX_VALUE, indentation);
        }

        return Console.getFont().wrapString(message, Console.getFontSize(),
                Console.getScrollbackWidth(), Float.MAX_VALUE, indentation);
    }

    /**
     * Indents and word-wraps a {@link String} to fit within the console's overlay.
     *
     * @param message     The {@link String} to be indented.
     * @param indentation The number of spaces to indent {@code message} with.
     *
     * @return {@code message}, indented and word-wrapped to fit within the console overlay.
     *
     * @since 3.0
     */
    public static String indent(String message, int indentation)
    {
        return indent(message, indentation, null);
    }

    @Nullable
    public static FactionAPI findBestFactionMatch(String name)
    {
        name = name.toLowerCase();
        FactionAPI bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        // Check IDs first in case multiple factions share the same name
        for (FactionAPI faction : Global.getSector().getAllFactions())
        {
            double distance = calcSimilarity(name, faction.getId().toLowerCase());

            if (distance == 1.0)
            {
                return faction;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = faction;
            }
        }

        // Search again by name if no matching ID is found
        if (bestMatch == null)
        {
            for (FactionAPI faction : Global.getSector().getAllFactions())
            {
                double distance = calcSimilarity(name, faction.getDisplayName().toLowerCase());

                if (distance == 1.0)
                {
                    return faction;
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = faction;
                }
            }
        }

        return bestMatch;
    }

    @Nullable
    public static MarketAPI findBestMarketMatch(String name)
    {
        name = name.toLowerCase();
        MarketAPI bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        // Check IDs first in case multiple markets share the same name
        final List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        for (MarketAPI market : markets)
        {
            double distance = calcSimilarity(name, market.getId().toLowerCase());

            if (distance == 1.0)
            {
                return market;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = market;
            }
        }

        // Search again by name if no matching ID is found
        if (bestMatch == null)
        {
            for (MarketAPI market : markets)
            {
                double distance = calcSimilarity(name, market.getName().toLowerCase());

                if (distance == 1.0)
                {
                    return market;
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = market;
                }
            }
        }

        return bestMatch;
    }

    @Nullable
    public static MarketConditionSpecAPI findBestMarketConditionMatch(String name)
    {
        if (Global.getSettings().getMarketConditionSpec(name) != null)
        {
            return Global.getSettings().getMarketConditionSpec(name);
        }

        name = name.toLowerCase();
        String bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        // Check IDs first in case multiple conditions share the same name
        final List<Pair<String, String>> pairs = List_.getMarketConditionIdsWithNames();
        for (Pair<String, String> pair : pairs)
        {
            final String id = pair.one;
            double distance = calcSimilarity(name, id.toLowerCase());

            if (distance == 1.0)
            {
                return Global.getSettings().getMarketConditionSpec(id);
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = id;
            }
        }

        // Search again by name if no matching ID is found
        if (bestMatch == null)
        {
            for (Pair<String, String> pair : pairs)
            {
                double distance = calcSimilarity(name, pair.two.toLowerCase());

                if (distance == 1.0)
                {
                    return Global.getSettings().getMarketConditionSpec(pair.one);
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = pair.one;
                }
            }
        }

        if (bestMatch == null)
        {
            return null;
        }

        return Global.getSettings().getMarketConditionSpec(bestMatch);
    }

    @Nullable
    public static IndustrySpecAPI findBestIndustryMatch(String name)
    {
        name = name.toLowerCase();
        IndustrySpecAPI bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        for (IndustrySpecAPI industry : Global.getSettings().getAllIndustrySpecs())
        {
            double distance = calcSimilarity(name, industry.getId().toLowerCase());

            if (distance == 1.0)
            {
                return industry;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = industry;
            }
        }

        // Search again by name if no matching ID is found
        if (bestMatch == null)
        {
            for (IndustrySpecAPI industry : Global.getSettings().getAllIndustrySpecs())
            {
                double distance = calcSimilarity(name, industry.getName().toLowerCase());

                if (distance == 1.0)
                {
                    return industry;
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = industry;
                }
            }
        }

        return bestMatch;
    }

    public static LocationAPI findBestLocationMatch(String name)
    {
        name = name.toLowerCase();
        LocationAPI bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        // Check IDs first in case multiple tokens share the same name
        for (LocationAPI loc : Global.getSector().getAllLocations())
        {
            double distance = calcSimilarity(name, loc.getId().toLowerCase());

            if (distance == 1.0)
            {
                return loc;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = loc;
            }
        }

        // Search again by name if no matching ID is found
        if (bestMatch == null)
        {
            for (LocationAPI loc : Global.getSector().getAllLocations())
            {
                double distance = calcSimilarity(name, loc.getName().toLowerCase());

                if (distance == 1.0)
                {
                    return loc;
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = loc;
                }
            }
        }

        return bestMatch;
    }

    @Nullable
    public static StarSystemAPI findBestSystemMatch(String name)
    {
        name = name.toLowerCase();
        StarSystemAPI bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        // Check IDs first in case multiple tokens share the same name
        for (StarSystemAPI loc : Global.getSector().getStarSystems())
        {
            double distance = calcSimilarity(name, loc.getId().toLowerCase());

            if (distance == 1.0)
            {
                return loc;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = loc;
            }
        }

        // Search again by name if no matching ID is found
        if (bestMatch == null)
        {
            for (StarSystemAPI loc : Global.getSector().getStarSystems())
            {
                double distance = calcSimilarity(name, loc.getBaseName().toLowerCase());

                if (distance == 1.0)
                {
                    return loc;
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = loc;
                }
            }
        }

        return bestMatch;
    }

    @Nullable
    public static SectorEntityToken findBestTokenMatch(String name,
                                                       Collection<SectorEntityToken> toSearch)
    {
        name = name.toLowerCase();
        SectorEntityToken bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        // Check IDs first in case multiple tokens share the same name
        for (SectorEntityToken token : toSearch)
        {
            double distance = calcSimilarity(name, token.getId().toLowerCase());

            if (distance == 1.0)
            {
                return token;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = token;
            }
        }

        // Search again by name/full name if no matching ID is found
        if (bestMatch == null)
        {
            for (SectorEntityToken token : toSearch)
            {
                double distance = Math.max(calcSimilarity(name, token.getName().toLowerCase()),
                        calcSimilarity(name, token.getFullName().toLowerCase()));

                if (distance == 1.0)
                {
                    return token;
                }

                if (distance > closestDistance)
                {
                    closestDistance = distance;
                    bestMatch = token;
                }
            }
        }

        return bestMatch;
    }

    @Nullable
    public static OfficerDataAPI findBestOfficerMatch(String name, CampaignFleetAPI fleet)
    {
        name = name.toLowerCase();
        OfficerDataAPI bestMatch = null;
        double closestDistance = Console.getSettings().getTypoCorrectionThreshold();

        for (OfficerDataAPI officer : fleet.getFleetData().getOfficersCopy())
        {
            double distance = calcSimilarity(name, officer.getPerson().getNameString().toLowerCase());

            if (distance == 1.0)
            {
                return officer;
            }

            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = officer;
            }
        }

        return (bestMatch == null ? null : fleet.getFleetData().getOfficerData(bestMatch.getPerson()));
    }

    public static List<String> findMatchingStrings(String pattern, List<String> haystack)
    {
        List<String> matching = new ArrayList<>();

        for (String hay : haystack)
        {
            if (hay.matches(pattern)) {
                matching.add(hay);
            }
        }

        return matching;
    }

    public static List<SectorEntityToken> getEntitiesWithTags(LocationAPI location,
                                                              String... tags)
    {
        final Set<SectorEntityToken> tokens = new HashSet<>();
        for (String tag : tags)
        {
            tokens.addAll(location.getEntitiesWithTag(tag));
        }
        return new ArrayList<>(tokens);
    }

    public static SectorEntityToken findTokenInLocation(String toFind, LocationAPI location)
    {
        return findTokenInLocation(toFind, location, Tags.COMM_RELAY, Tags.GATE,
                Tags.JUMP_POINT, Tags.PLANET, Tags.STAR, Tags.STATION, Tags.WRECK);
    }

    @SuppressWarnings("deprecation")
    private static SectorEntityToken findTokenInLocation(String toFind,
                                                         LocationAPI location, String... validTags)
    {
        SectorEntityToken tmp = location.getEntityByName(toFind);

        if (tmp == null)
        {
            tmp = location.getEntityById(toFind);
        }

        if (tmp == null && ENABLE_TYPO_CORRECTION)
        {
            final Set<SectorEntityToken> toSearch = new HashSet<>();
            for (String tag : validTags)
            {
                toSearch.addAll(location.getEntitiesWithTag(tag));
            }

            tmp = findBestTokenMatch(toFind, toSearch);
        }

        return tmp;
    }

    public static String getFactionName(FactionAPI faction)
    {
        if (faction.isPlayerFaction())
        {
            return "Player";
        }

        return faction.getDisplayName();
    }

    // Returns the best cargo to add player-accessible items to
    public static CargoAPI getUsableCargo(SectorEntityToken token)
    {
        // Always return fleet's cargo
        if (token instanceof FleetMemberAPI)
        {
            return token.getCargo();
        }

        // Return storage tab's cargo, if present
        final MarketAPI market = token.getMarket();
        if (market != null && !market.getSubmarketsCopy().isEmpty())
        {
            SubmarketAPI submarket = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
            if (submarket != null)
            {
                return submarket.getCargo();
            }

            // Return first other market found
            return market.getSubmarketsCopy().get(0).getCargo();
        }

        // Fallback, won't work in some cases
        return token.getCargo();
    }

    private CommandUtils()
    {
    }
}
