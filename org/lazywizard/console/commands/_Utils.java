package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

class _Utils
{
    private static final boolean ENABLE_TYPO_CORRECTION = true;

    /**
     * Returns normalized score, with 0.0 meaning no similarity at all,
     * and 1.0 meaning full equality.
     */
    // Taken from: https://code.google.com/p/duke/source/browse/src/main/java/no/priv/garshol/duke/comparators/JaroWinkler.java
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
        for (; p < last && s1.charAt(p) == s2.charAt(p); p++)
      ;

        score = score + ((p * (1 - score)) / 10);

        // (3) longer string adjustment
        // I'm confused about this part. Winkler's original source code includes
        // it, and Yancey's 2005 paper describes it. However, Winkler's list of
        // test cases in his 2006 paper does not include this modification. So
        // is this part of Jaro-Winkler, or is it not? Hard to say.
        //
        //   if (s1.length() >= 5 && // both strings at least 5 characters long
        //       c - p >= 2 && // at least two common characters besides prefix
        //       c - p >= ((s1.length() - p) / 2)) // fairly rich in common chars
        //     {
        //     System.out.println("ADJUSTED!");
        //     score = score + ((1 - score) * ((c - (p + 1)) /
        //                                     ((double) ((s1.length() + s2.length())
        //                                                - (2 * (p - 1))))));
        // }
        // (4) similar characters adjustment
        // the same holds for this as for (3) above.
        return score;
    }

    // Stolen from Wikipedia
    private static int calcLevenshteinDistance(String s, String t)
    {
        s = s.toLowerCase();
        t = t.toLowerCase();
        // degenerate cases
        if (s.equals(t))
        {
            return 0;
        }
        if (s.length() == 0)
        {
            return t.length();
        }
        if (t.length() == 0)
        {
            return s.length();
        }

        // create two work vectors of integer distances
        int[] v0 = new int[t.length() + 1];
        int[] v1 = new int[t.length() + 1];

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < v0.length; i++)
        {
            v0[i] = i;
        }

        for (int i = 0; i < s.length(); i++)
        {
            // calculate v1 (current row distances) from the previous row v0
            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < t.length(); j++)
            {
                int cost = (s.charAt(i) == t.charAt(j)) ? 0 : 1;
                v1[j + 1] = Math.min(Math.min(v1[j] + 1, v0[j + 1] + 1), v0[j] + cost);
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            System.arraycopy(v1, 0, v0, 0, v0.length);
        }

        return v1[t.length()];
    }

    public static void main(String[] args)
    {
        String word1 = "corvus_comm_relay";
        String word2 = "corvas_com_rillay";
        System.out.println(calcLevenshteinDistance(word1, word2));
        System.out.println(calcSimilarity(word1, word2));
    }

    static String findBestStringMatch(String id, Collection<String> toSearch)
    {
        if (toSearch.contains(id))
        {
            return id;
        }

        id = id.toLowerCase();
        String bestMatch = null;
        double closestDistance = .85f; // Threshold

        for (String str : toSearch)
        {
            double distance = calcSimilarity(id, str);
            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = str;
            }
        }

        return bestMatch;
    }

    static SectorEntityToken findBestTokenMatch(String name, Collection<SectorEntityToken> toSearch)
    {
        name = name.toLowerCase();
        SectorEntityToken bestMatch = null;
        double closestDistance = .85f; // Threshold

        for (SectorEntityToken token : toSearch)
        {
            double distance = Math.max(Math.max(
                    calcSimilarity(name, token.getId().toLowerCase()),
                    calcSimilarity(name, token.getName().toLowerCase())),
                    calcSimilarity(name, token.getFullName().toLowerCase()));
            if (distance > closestDistance)
            {
                closestDistance = distance;
                bestMatch = token;
            }
        }

        return bestMatch;
    }

    static List<SectorEntityToken> getEntitiesWithTags(LocationAPI location,
            String... tags)
    {
        Set<SectorEntityToken> tokens = new HashSet<>();
        for (String tag : tags)
        {
            tokens.addAll(location.getEntitiesWithTag(tag));
        }
        return new ArrayList<>(tokens);
    }

    static SectorEntityToken findTokenInLocation(String toFind, LocationAPI location)
    {
        return findTokenInLocation(toFind, location, Tags.COMM_RELAY,
                Tags.JUMP_POINT, Tags.PLANET, Tags.STAR, Tags.STATION);
    }

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
            Set<SectorEntityToken> toSearch = new HashSet<>();
            for (String tag : validTags)
            {
                toSearch.addAll(location.getEntitiesWithTag(tag));
            }

            tmp = findBestTokenMatch(toFind, toSearch);
        }

        return tmp;
    }

    private _Utils()
    {
    }
}
