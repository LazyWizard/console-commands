package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import data.scripts.console.commands.*;
import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Executes commands and handles console output. Can't be instantiated.
 *
 * @see ConsoleManager
 */
public final class Console
{
    // All command implementations must be in this package!
    private static final String COMMAND_PACKAGE = "data.scripts.console.commands";
    // Constants to define console output appearance
    private static final Color CONSOLE_COLOR = Color.YELLOW;
    private static final int LINE_LENGTH = 80;
    // Maps the command to the associated class
    private static final Map allCommands = new TreeMap();
    private static final Set hardcodedCommands = new HashSet();
    // The ConsoleManager that requested input (per-save console settings)
    private static WeakReference activeManager;

    // Everything in this block absolutely MUST compile or the console will crash
    static
    {
        // Change the look and feel of the console pop-up
        UIManager.put("Panel.background", Color.BLACK);
        UIManager.put("OptionPane.background", Color.BLACK);
        UIManager.put("OptionPane.messageForeground", Color.CYAN);
        UIManager.put("TextField.background", Color.BLACK);
        UIManager.put("TextField.foreground", Color.YELLOW);
        UIManager.put("Button.background", Color.BLACK);
        UIManager.put("Button.foreground", Color.LIGHT_GRAY);

        // Since I know these classes pass validation, we can insert them
        // directly into the command map instead of through registerCommand
        allCommands.put("runscript", RunScript.class);
        allCommands.put("runcode", RunCode.class);
        allCommands.put("spawnfleet", SpawnFleet.class);
        allCommands.put("addship", AddShip.class);
        allCommands.put("addwing", AddWing.class);
        allCommands.put("addcredits", AddCredits.class);
        allCommands.put("addfuel", AddFuel.class);
        allCommands.put("addsupplies", AddSupplies.class);
        allCommands.put("setrelationship", SetRelationship.class);
        allCommands.put("adjustrelationship", AdjustRelationship.class);
        allCommands.put("addweapon", AddWeapon.class);
        allCommands.put("additem", AddItem.class);
        allCommands.put("addcrew", AddCrew.class);
        allCommands.put("addmarines", AddMarines.class);
        allCommands.put("allweapons", AllWeapons.class);
        allCommands.put("goto", GoTo.class);
        allCommands.put("home", Home.class);
        allCommands.put("sethome", SetHome.class);
        allCommands.put("gc", GC.class);
        allCommands.put("addcp", AddCP.class);

        // Commands that can't be overwritten
        hardcodedCommands.add("help");
        hardcodedCommands.add("status");
        hardcodedCommands.add("runtests");
        hardcodedCommands.addAll(allCommands.keySet());
    }

    private Console()
    {
    }

    static void registerCommand(Class commandClass) throws Exception
    //throws InvalidCommandObjectException, InvalidCommandPackageException
    {
        String command = commandClass.getSimpleName().toLowerCase();

        if (hardcodedCommands.contains(command))
        {
            // InvalidCommandNameException
            throw new Exception("Can't overwrite built-in commands!");
        }

        // getPackage() won't work for classes compiled with Janino's classloader
        // There's an extremely ugly workaround below
        //if (!COMMAND_PACKAGE.equals(commandClass.getPackage().getName()))
        if (!COMMAND_PACKAGE.equals(commandClass.getCanonicalName().substring(0,
                commandClass.getCanonicalName().lastIndexOf('.'))))
        {
            // InvalidCommandPackageException
            throw new Exception("Console command "
                    + commandClass.getCanonicalName() + " is not in the '"
                    + COMMAND_PACKAGE + "' package!");
        }

        if (!BaseCommand.class.isAssignableFrom(commandClass))
        {
            // InvalidCommandObjectException
            throw new Exception("Console command "
                    + commandClass.getCanonicalName()
                    + " does not extend BaseCommand!");
        }

        if (allCommands.put(command, commandClass) != null)
        {
            showMessage("Replaced existing command '" + command + "'.");
        }
    }

    static void getInput()
    {
        //JOptionPane.showMessageDialog(null, "Thread: "
        //        + Thread.currentThread().getName());
        parseCommand(JOptionPane.showInputDialog(null,
                "Enter a console command (or 'help' for a list of valid commands):",
                "Starfarer Console", JOptionPane.PLAIN_MESSAGE));
    }

    static void getInput(ConsoleManager manager)
    {
        setManager(manager);
        getInput();
    }

    private static void runTests()
    {
        Global.getSector().addMessage("Running console tests...");
        //parseCommand("runscript help");
        //parseCommand("runscript list");
        //parseCommand("spawnfleet hegemony fake");
        parseCommand("spawnfleet hegemony supplyConvoy");
        parseCommand("addcredits 500000");
        parseCommand("addsupplies 500");
        parseCommand("addfuel 500");
        parseCommand("addship atlas_Hull");
        parseCommand("addwing talon_wing");
        parseCommand("setrelationship player hegemony 5");
        parseCommand("adjustrelationship player hegemony -5.1");
        parseCommand("adjustrelationship player hegemo53ny -5.1");
        parseCommand("runcode Global.getSector().addMessage(\"Test\");");
    }

    private static void showStatus()
    {
        try
        {
            showMessage("Console status:",
                    "Thread: " + Thread.currentThread().getName()
                    + "\nIn campaign: "
                    + (getManager() != null ? "yes" : "no")
                    + "\nIn battle: "
                    + (ConsoleManager.getCombatEngine() != null ? "yes" : "no"),
                    true);
        }
        catch (Exception ex)
        {
            showError("Error showing status: ", ex);
        }
    }

    static ConsoleManager getManager()
    {
        if (activeManager == null || activeManager.get() == null)
        {
            return null;
        }

        return (ConsoleManager) activeManager.get();
    }

    static void setManager(ConsoleManager manager)
    {
        activeManager = new WeakReference(manager);
    }

    private static void listCommands()
    {
        StringBuilder names = new StringBuilder("Help, Status");
        Iterator iter = allCommands.values().iterator();
        Class tmp;

        while (iter.hasNext())
        {
            names.append(", ");
            tmp = (Class) iter.next();
            names.append(tmp.getSimpleName());
        }

        showMessage("Valid commands (not case-sensitive): ",
                names.toString(), true);
        showMessage("Running a command with the argument"
                + " 'help' will display more detailed instructions on how to"
                + " use that command.");
    }

    private static boolean parseCommand(String command)
    {
        // Don't try to parse blank lines
        if (command == null || command.length() == 0)
        {
            return false;
        }

        String[] args = command.split(" ");
        String com = args[0].toLowerCase();

        Global.getSector().addMessage("Running command '" + command + "'.",
                command, Color.GREEN);

        if (com.equals("runtests"))
        {
            runTests();
            return true;
        }

        if (com.equals("status"))
        {
            showStatus();
            return true;
        }

        if (com.equals("list"))
        {
            listCommands();
            return true;
        }

        if (com.equals("help"))
        {
            if (args.length == 2)
            {
                com = args[1];
                args[1] = "help";
            }
            else
            {
                listCommands();
                return false;
            }
        }

        if (args.length > 1)
        {
            StringBuilder arg = new StringBuilder();

            for (int x = 1; x < args.length; x++)
            {
                if (x != 1)
                {
                    arg.append(" ");
                }

                arg.append(args[x]);
            }

            return executeCommand(com, arg.toString());
        }

        return executeCommand(com, "");
    }

    static void addScript(String name, Script script)
    {
        RunScript.addScript(name, script);
    }

    private static synchronized boolean executeCommand(String com, String args)
    {
        BaseCommand command;

        if (allCommands.containsKey(com))
        {
            try
            {
                command = (BaseCommand) ((Class) allCommands.get(com)).newInstance();
            }
            catch (InstantiationException ex)
            {
                showError("Error while retrieving command "
                        + com + ": ", ex);
                return false;
            }
            catch (IllegalAccessException ex)
            {
                showError("Error while retrieving command "
                        + com + ": ", ex);
                return false;
            }
            catch (ClassCastException ex)
            {
                showError("Error while retrieving command "
                        + com + ": ", ex);
                return false;
            }
        }
        else
        {
            showMessage("No such command '" + com + "'!");
            listCommands();
            return false;
        }

        if ("help".equals(args))
        {
            command.showHelp();
            return true;
        }

        /*if (command.isCampaignOnly() && Global.getSector().getPlayerFleet() == null)
         {
         showMessage("This command can only be run in a campaign!");
         return false;
         }*/

        if (command.isCombatOnly() && !ConsoleManager.isInBattle())
        {
            showMessage("This command can only be run during combat!");
            return false;
        }

        try
        {
            return command.runCommand(args);
        }
        catch (Exception ex)
        {
            command.showSyntax();
            showError("Error while running command: ", ex);
            return false;
        }
    }

    /**
     * Formats and word-wraps the supplied text, then outputs it to the player.
     *
     * @param preamble the header for this message, won't be indented
     * @param message the main body of text
     * @param indent whether to indent each line of the main body
     */
    public static void showMessage(String preamble,
            String message, boolean indent)
    {
        if (preamble != null)
        {
            showMessage(preamble);
        }

        // Analyse each line of the message seperately
        String[] lines = message.split("\n");
        StringBuilder line = new StringBuilder(LINE_LENGTH);

        // Word wrapping is complicated ;)
        for (int x = 0; x < lines.length; x++)
        {
            // Check if the string even needs to be broken up
            if (lines[x].length() > LINE_LENGTH)
            {
                // Clear the StringBuilder so we can generate a new line
                line.setLength(0);
                // Split the line up into the individual words, and append each
                // word to the next line until the character limit is reached
                String[] words = lines[x].split(" ");
                for (int y = 0; y < words.length; y++)
                {
                    // If this word by itself is longer than the line limit,
                    // just go ahead and post it in its own line
                    if (words[y].length() > LINE_LENGTH)
                    {
                        // Make sure to post the previous line in queue, if any
                        if (line.length() > 0)
                        {
                            printLine(line.toString(), indent);
                            line.setLength(0);
                        }

                        printLine(words[y], indent);
                    }
                    // If this word would put us over the length limit, post
                    // the queue and back up a step (re-check this word with
                    // a blank line - this is in case it trips the above block)
                    else if (words[y].length() + line.length() > LINE_LENGTH)
                    {
                        printLine(line.toString(), indent);
                        line.setLength(0);
                        y--;
                    }
                    // This word won't put us over the limit, add it to the queue
                    else
                    {
                        line.append(words[y]);
                        line.append(" ");

                        // If we have reached the end of the message, ensure
                        // that we post the remaining part of the queue
                        if (y == (words.length - 1))
                        {
                            printLine(line.toString(), indent);
                        }
                    }
                }
            }
            // Entire message fits into a single line
            else
            {
                printLine(lines[x], indent);
            }
        }
    }

    /**
     * Formats and word-wraps the supplied text, then outputs it to the player.
     *
     * @param message the message to output
     */
    public static void showMessage(String message)
    {
        showMessage(null, message, false);
    }

    private static void showError(String preamble, Exception ex)
    {
        if (preamble == null)
        {
            preamble = "Error: ";
        }
        else if (!preamble.endsWith(": "))
        {
            preamble = preamble + ": ";
        }

        showMessage(preamble + ex.toString(), ex.getMessage(), true);
    }

    private static void printLine(String message, boolean indent)
    {
        if (ConsoleManager.isInBattle())
        {
            // No in-battle message hooks yet
        }
        else
        {
            if (indent)
            {
                Global.getSector().addMessage("   " + message, CONSOLE_COLOR);
            }
            else
            {
                Global.getSector().addMessage(message, CONSOLE_COLOR);
            }
        }
    }
}