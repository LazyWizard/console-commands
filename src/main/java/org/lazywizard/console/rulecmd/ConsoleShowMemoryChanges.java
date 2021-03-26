package org.lazywizard.console.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.Console;

import java.util.*;

public class ConsoleShowMemoryChanges extends BaseCommandPlugin
{
    private static final Map<String, String> RULE_MAP = new HashMap<>();

    static
    {
        RULE_MAP.put("consoleDebugNGCOpen", "BeginNewGameCreation");
        RULE_MAP.put("consoleDebugNGCSelected", "NewGameOptionSelected");
        RULE_MAP.put("consoleDebugOpen", "OpenInteractionDialog");
        RULE_MAP.put("consoleDebugSelected", "DialogOptionSelected");
        RULE_MAP.put("consoleDebugOpenComms", "OpenCommLink");
        RULE_MAP.put("consoleDebugFleetEncounter", "BeginFleetEncounter");
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        final String matchingRule = RULE_MAP.get(ruleId);
        if (matchingRule == null) throw new RuntimeException("No match for rule '" + ruleId + '!');

        // Execute the rule we've intercepted, and monitor for any memory changes
        ConsoleShouldIntercept.setIntercepting(false);
        final Map<String, Map<String, Object>> oldMemory = takeSnapshot(memoryMap);
        FireBest.fire(ruleId, dialog, memoryMap, matchingRule);
        final Map<String, Map<String, Object>> newMemory = takeSnapshot(memoryMap);
        ConsoleShouldIntercept.setIntercepting(true);

        printMemoryChanges(dialog, oldMemory, newMemory);

        final String option = Misc.replaceTokensFromMemory("$option", memoryMap);
        if (!"$option".equals(option))
        {
            dialog.getTextPanel().addParagraph("[Option ran: " + option + "]");
        }

        return false;
    }

    private static Map<String, Map<String, Object>> takeSnapshot(Map<String, MemoryAPI> memoryMap)
    {
        final Map<String, Map<String, Object>> clonedMemory = new TreeMap<>();
        for (Map.Entry<String, MemoryAPI> entry : memoryMap.entrySet())
        {
            final String key = entry.getKey();
            final MemoryAPI subMemory = entry.getValue();
            final Map<String, Object> asMap = new TreeMap<>();
            for (String subKey : subMemory.getKeys())
            {
                asMap.put(subKey, subMemory.get(subKey));
            }

            clonedMemory.put(key, asMap);
        }

        return clonedMemory;
    }

    // Beautify certain API types that lack a readable toString()
    private static String asString(Object object)
    {
        if (object == null) return "null";
        if (object instanceof SectorEntityToken) return ((SectorEntityToken) object).getFullName();
        if (object instanceof PersonAPI) return ((PersonAPI) object).getNameString();
        return object.toString();
    }

    private static String asString(String category, String key, Object object, boolean excludeCategory)
    {
        if (category.startsWith("$")) category = category.substring(1);
        if (key.startsWith("$")) key = key.substring(1);

        if (excludeCategory) return "$" + key + " = " + asString(object);

        return "$" + category + "." + key + " = " + asString(object);
    }

    // TODO: Sort memory map for better readability
    private static void printMemoryChanges(InteractionDialogAPI dialog, Map<String, Map<String, Object>> oldMemory, Map<String, Map<String, Object>> newMemory)
    {
        // Ensures no categories exist on only one side, to avoid ugly checks later
        final Set<String> categories = new LinkedHashSet<>();
        for (String key : oldMemory.keySet())
        {
            categories.add(key);
            if (!newMemory.containsKey(key))
            {
                newMemory.put(key, new TreeMap<String, Object>());
            }
        }
        for (String key : newMemory.keySet())
        {
            if (!oldMemory.containsKey(key))
            {
                categories.add(key);
                oldMemory.put(key, new TreeMap<String, Object>());
            }
        }

        final StringBuilder report = new StringBuilder();
        for (String category : categories)
        {
            final boolean isLocal = MemKeys.LOCAL.equals(category);
            final Map<String, Object> oldMem = oldMemory.get(category),
                    newMem = newMemory.get(category);
            for (Map.Entry<String, Object> entry : oldMem.entrySet())
            {
                final String key = entry.getKey();
                if (!newMem.containsKey(key))
                {
                    report.append("Removed entry: ").append(asString(category, key, entry.getValue(), isLocal)).append("\n");
                }
            }
            for (Map.Entry<String, Object> entry : newMem.entrySet())
            {
                final String key = entry.getKey();
                if (!oldMem.containsKey(key))
                {
                    report.append("New entry: ").append(asString(category, key, entry.getValue(), isLocal)).append("\n");
                }
                else
                {
                    final Object oldValue = oldMem.get(key), newValue = entry.getValue();
                    if (!Objects.equals(newValue, oldValue))
                    {
                        report.append("Changed value: ").append(asString(category, key, newValue, isLocal))
                                .append(", was ").append(asString(oldValue)).append("\n");
                    }
                }
            }
        }

        if (report.length() > 0)
        {
            dialog.getTextPanel().addParagraph(report.toString(), Console.getSettings().getOutputColor());
        }
        else
        {
            dialog.getTextPanel().addParagraph("No memory changes detected.", Console.getSettings().getOutputColor());
        }
    }
}
