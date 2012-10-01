package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import data.scripts.console.commands.*;
import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

final class Console
{
    // All command implementations must be in this package!
    private static final String COMMAND_PACKAGE = "data.scripts.console.commands";
    // Constants to define console output appearance
    private static final Color CONSOLE_COLOR = Color.YELLOW;
    private static final int LINE_LENGTH = 80;
    // Maps the command to the associated class
    private static final SortedSet allCommands = new TreeSet();
    // The ConsoleManager that requested input (cheap multi-system support)
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

        allCommands.add("RunScript");
        allCommands.add("SpawnFleet");
        allCommands.add("AddShip");
        allCommands.add("AddWing");
        allCommands.add("AddCredits");
        allCommands.add("AddFuel");
        allCommands.add("AddSupplies");
        allCommands.add("SetRelationship");
        allCommands.add("AdjustRelationship");
        allCommands.add("AddItem");
        allCommands.add("AddWeapon");
        allCommands.add("AddCrew");
        allCommands.add("AddMarines");
        allCommands.add("AllWeapons");
        allCommands.add("GoTo");
        allCommands.add("Home");
        allCommands.add("SetHome");
        allCommands.add("GC");
    }

    private Console()
    {
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

    public static ConsoleManager getManager()
    {
        return (ConsoleManager) activeManager.get();
    }

    static void setManager(ConsoleManager manager)
    {
        activeManager = new WeakReference(manager);
    }

    public static void listCommands()
    {
        StringBuilder names = new StringBuilder("Help");
        Iterator iter = allCommands.iterator();

        while (iter.hasNext())
        {
            names.append(", ");
            names.append((String) iter.next());
        }

        showMessage("Valid commands (not case-sensitive): ",
                names.toString(), true);
        showMessage("Running a command with the argument"
                + " 'help' will display more detailed instructions on how to"
                + " use that command.");
    }

    public static void getInput(ConsoleManager manager)
    {
        setManager(manager);
        parseCommand(JOptionPane.showInputDialog(null,
                "Enter a console command (or 'help' for a list of valid commands):",
                "Starfarer Console", JOptionPane.PLAIN_MESSAGE));
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

        return executeCommand(com);
    }

    public static void addScript(String name, Script script)
    {
        RunScript.addScript(name, script);
    }

    // Commands with arguments
    private static boolean executeCommand(String com, String args)
    {
        BaseCommand command;

        if (com.equals("runscript"))
        {
            command = new RunScript();
        }
        else if (com.equals("spawnfleet"))
        {
            command = new SpawnFleet();
        }
        else if (com.equals("addship"))
        {
            command = new AddShip();
        }
        else if (com.equals("addwing"))
        {
            command = new AddWing();
        }
        else if (com.equals("addcredits"))
        {
            command = new AddCredits();
        }
        else if (com.equals("addfuel"))
        {
            command = new AddFuel();
        }
        else if (com.equals("addsupplies"))
        {
            command = new AddSupplies();
        }
        else if (com.equals("setrelationship"))
        {
            command = new SetRelationship();
        }
        else if (com.equals("adjustrelationship"))
        {
            command = new AdjustRelationship();
        }
        else if (com.equals("addweapon"))
        {
            command = new AddWeapon();
        }
        else if (com.equals("additem"))
        {
            command = new AddItem();
        }
        else if (com.equals("addcrew"))
        {
            command = new AddCrew();
        }
        else if (com.equals("addmarines"))
        {
            command = new AddMarines();
        }
        else if (com.equals("allweapons"))
        {
            command = new AllWeapons();
        }
        else if (com.equals("goto"))
        {
            command = new GoTo();
        }
        else if (com.equals("home"))
        {
            command = new Home();
        }
        else if (com.equals("sethome"))
        {
            command = new SetHome();
        }
        else if (com.equals("gc"))
        {
            command = new GC();
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

        try
        {
            return command.runCommand(args);
        }
        catch (Exception ex)
        {
            command.showSyntax();
            showMessage("Error while running command "
                    + com + ":", ex.getMessage(), true);
            return false;
        }
    }

    // Commands without arguments
    private static boolean executeCommand(String com)
    {
        // Not supported yet
        return executeCommand(com, "");
    }

    public static void showMessage(String preamble,
            String message, boolean indent)
    {
        if (preamble != null)
        {
            Global.getSector().addMessage(preamble, CONSOLE_COLOR);
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

    public static void showMessage(String message, boolean indent)
    {
        showMessage(null, message, indent);
    }

    public static void showMessage(String message)
    {
        showMessage(null, message, false);
    }

    private static void printLine(String message, boolean indent)
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