package org.lazywizard.console;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.overlay.legacy.LegacyConsoleOverlay;
import org.lazywizard.console.overlay.legacy.ConsoleOverlayInternal;
import org.lazywizard.console.overlay.v2.panels.ConsoleOverlayPanel;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The main class of the console mod. Most of its methods aren't publicly
 * accessible, so this is mainly used to display messages to the player.
 *
 * @author LazyWizard
 * @since 2.0
 */
public class Console
{
    private static final Logger Log = Global.getLogger(Console.class);
    private static LazyFont font;
    // Stores the output of the console until it can be displayed
    private static StringBuilder output = new StringBuilder();
    private static CommandContext currentContext = CommandContext.COMBAT_MISSION;

    /**
     * Forces the console to reload its settings from the settings file.
     *
     * @throws IOException   if the JSON file at
     *                       {@link CommonStrings#PATH_SETTINGS} does not exist
     *                       or can't be opened.
     * @throws JSONException if the JSON is malformed or missing entries.
     * @since 2.0
     */
    public static void reloadSettings() throws IOException, JSONException
    {
        final JSONObject settingsFile = Global.getSettings().loadJSON(CommonStrings.PATH_SETTINGS);

        // The sprite font used by the console overlay
        try
        {
            font = LazyFont.loadFont(settingsFile.getString("consoleFont"));
        }
        catch (FontException ex)
        {
            throw new RuntimeException("Failed to load sprite font!", ex);
        }

        // What level to log console output at
        final Level logLevel = Level.toLevel(settingsFile.getString("consoleLogLevel"), Level.WARN);
        Global.getLogger(Console.class).setLevel(logLevel);
        Global.getLogger(CommandStore.class).setLevel(logLevel);
    }

    public static ConsoleSettings getSettings()
    {
        return ConsoleSettings.INSTANCE;
    }

    public static LazyFont getFont()
    {
        return font;
    }

    public static CommandContext getContext()
    {
        return currentContext;
    }

    public static float getFontSize()
    {
        return font.getBaseHeight() * getSettings().getFontScaling();
    }

    public static float getScrollbackWidth()
    {
        return (Display.getWidth() * Display.getPixelScaleFactor()) - LegacyConsoleOverlay.HORIZONTAL_MARGIN * 2f;
    }

    static Object getCommandTarget(CommandContext context)
    {
        return (context.isInCombat() ? Global.getCombatEngine().getPlayerShip().getShipTarget()
                : Global.getSector().getPlayerFleet().getInteractionTarget());
    }

    //<editor-fold defaultstate="collapsed" desc="showMessage variants">

    /**
     * Displays a message to the user. The message will be formatted and shown
     * to the player when they reach a section of the game where it can be
     * displayed properly (combat/campaign map).
     *
     * @param message  The message to show.
     * @param logLevel If this is equal to/higher than the "consoleLogLevel"
     *                 setting, this message will be logged in Starsector.log.
     *
     * @since 2.0
     */
    public static void showMessage(Object message, Level logLevel)
    {
        // Add message to the output queue
        output.append('\n').append(message);

        ConsoleOverlayPanel.setOutput(ConsoleOverlayPanel.getOutput() +message.toString() +  "\n" );


        // Also add to Starsector's log
        Log.log(logLevel, message);
    }

    /**
     * Displays a message to the user. The message will be formatted and shown
     * to the player when they reach a section of the game where it can be
     * displayed properly (combat/campaign map).
     *
     * @param message The message to show.
     *
     * @since 2.0
     */
    public static void showMessage(Object message)
    {
        showMessage(message, Level.INFO);
    }

    /**
     * Displays an indented message to the user. The message will be formatted and shown
     * to the player when they reach a section of the game where it can be
     * displayed properly (combat/campaign map).
     *
     * @param preamble    An optional argument; this part of the message will not be indented.
     * @param message     The indented message to show.
     * @param indentation The number of spaces to indent {@code message} with.
     *
     * @since 3.0
     */
    public static void showIndentedMessage(@Nullable String preamble, Object message, int indentation)
    {
        if (preamble != null && !preamble.isEmpty())
        {
            showMessage(preamble);
        }

        showMessage(CommandUtils.indent(message.toString(), indentation), Level.INFO);
    }

    private static String findClassSource(String className)
    {
        try
        {
            final Class srcClass = Class.forName(className, false, Global.getSettings().getScriptClassLoader());
            final CodeSource cs = srcClass.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null)
            {
                if (className.startsWith("data."))
                {
                    return "loose script";
                }
                else
                {
                    return "core Java";
                }
            }
            else
            {
                final String classSource = cs.getLocation().getFile().replace("\\", "/");
                if (classSource.endsWith(".jar"))
                {
                    return classSource.substring(classSource.lastIndexOf('/') + 1);
                }

                return classSource;
            }
        }
        // Classloader blocked - will probably always be a core Java class
        catch (SecurityException ex2)
        {
            return "core Java";
        }
        catch (ClassNotFoundException ex1)
        {
            return "synthetic class";
        }
    }

    /**
     * Displays the stack trace of a {@link Throwable}.
     *
     * @param message An optional message to show before the stack trace. Can be
     *                {@code null}.
     * @param ex      The {@link Throwable} whose stack trace will be shown.
     *
     * @since 2.0
     */
    public static void showException(Object message, Throwable ex)
    {
        final StringBuilder stackTrace = new StringBuilder(256);

        // Add message if one was entered
        if (message != null)
        {
            stackTrace.append(message);
            if (!message.toString().endsWith("\n"))
            {
                stackTrace.append("\n");
            }
        }

        // Add stack trace of Throwable, with a few extra details
        stackTrace.append(ex.toString()).append("\n");
        if (getSettings().getShowExceptionDetails())
        {
            for (StackTraceElement ste : ex.getStackTrace())
            {
                final String classSource = findClassSource(ste.getClassName());
                stackTrace.append("   [").append(classSource).append("]   at ").append(ste.toString()).append("\n");
            }
        }
        else
        {
            Log.error("Console ran into exception: ", ex);
        }

        showMessage(stackTrace.toString(), Level.ERROR);
    }

    public static void showDialogOnClose(InteractionDialogPlugin dialog,
                                         SectorEntityToken token)
    {
        Global.getSector().addTransientScript(new ShowDialogOnCloseScript(dialog, token));
    }

    public static void showDialogOnClose(SectorEntityToken token)
    {
        Global.getSector().addTransientScript(new ShowDialogOnCloseScript(null, token));
    }
    //</editor-fold>

    private static CommandResult runCommand(String input, CommandContext context)
    {
        // Split input into command and arguments
        String[] tmp = input.split(" ", 2);
        String com = tmp[0].toLowerCase();
        String args = (tmp.length > 1 ? tmp[1] : "");
        CommandResult result;

        // Support for clearing the overlay history
        if ("clear".equals(com))
        {
            output.setLength(0);
            LegacyConsoleOverlay.clear();
            return CommandResult.SUCCESS;
        }

        // Alias with arguments support
        if (CommandStore.getAliases().containsKey(com))
        {
            final String rawAlias = CommandStore.getAliases().get(com);
            tmp = rawAlias.split(" ", 2);
            com = tmp[0];
            if (tmp.length > 1)
            {
                args = tmp[1] + (args.isEmpty() ? "" : " " + args);
            }
        }

        final List<CommandListener> listeners = CommandStore.getListeners();
        CommandListener interceptor = null;
        try
        {
            StoredCommand stored = CommandStore.retrieveCommand(com);
            if (stored == null)
            {
                final String bestMatch = CommandUtils.findBestStringMatch(com,
                        CommandStore.getLoadedCommands());
                if (bestMatch != null)
                {
                    showMessage("No such command \"" + com + "\" registered,"
                            + " did you mean \"" + bestMatch + "\"?");
                    return CommandResult.ERROR;
                }

                showMessage("No such command \"" + com + "\" registered!", Level.ERROR);
                return CommandResult.ERROR;
            }

            if (getSettings().getShowEnteredCommands())
            {
                showMessage("> " + input);
            }

            // Command listener support
            for (CommandListener listener : listeners)
            {
                // Listeners are given the opportunity to take over execution of the command
                if (listener.onPreExecute(com, args, context, (interceptor != null)))
                {
                    if (interceptor == null) interceptor = listener;
                }
            }

            if (interceptor != null)
            {
                result = interceptor.execute(com, args, context);
            }
            else
            {
                final BaseCommand command = stored.getCommandClass().newInstance();
                result = command.runCommand(args, context);
            }

            if (result == CommandResult.BAD_SYNTAX
                    && !stored.getSyntax().isEmpty())
            {
                showMessage("Syntax: " + stored.getSyntax());
            }
        }
        catch (Exception ex)
        {
            showException("Failed to execute command \"" + input
                    + "\" in context " + context, ex);
            result = CommandResult.ERROR;
        }

        for (CommandListener listener : listeners)
        {
            listener.onPostExecute(com, args, result, context, interceptor);
        }

        return result;
    }

    public static void parseInput(String rawInput, CommandContext context)
    {
        if (rawInput == null)
        {
            return;
        }

        // Runcode and Alias ignore separators
        // Hopefully the ONLY hardcoded command support I'll add to this mod...
        String toLower = rawInput.toLowerCase();
        if (toLower.startsWith("runcode") || toLower.startsWith("alias"))
        {
            runCommand(rawInput, context);
        }
        else
        {
            // Split the raw input up into the individual commands
            // The command separator is used to separate multiple commands
            final String separator = Pattern.quote(getSettings().getCommandSeparator());
            final Set<CommandResult> results = new HashSet<>();
            final Map<String, String> aliases = CommandStore.getAliases();
            for (String input : rawInput.split(separator))
            {
                input = input.trim();
                toLower = input.toLowerCase();
                if (!input.isEmpty())
                {
                    // Regular commands
                    if (!aliases.containsKey(toLower))
                    {
                        results.add(runCommand(input, context));
                    }
                    // Whole-line alias support
                    else
                    {
                        final String comm = aliases.get(toLower);
                        // Runcode always passes all arguments as one command
                        if (comm.toLowerCase().startsWith("runcode "))
                        {
                            results.add(runCommand(comm, context));
                        }
                        // Regular alias, split into separate commands if needed
                        else
                        {
                            for (String input2 : comm.replace(";",
                                    getSettings().getCommandSeparator()).split(separator))
                            {
                                input2 = input2.trim();
                                if (!input2.isEmpty())
                                {
                                    results.add(runCommand(input2, context));
                                }
                            }
                        }
                    }
                }
            }
        }

        ConsoleOverlayInternal.setLastCommand(rawInput);
    }

    private static void showOutput(ConsoleListener listener)
    {
        if (output.length() > 0 && listener.showOutput(output.toString()))
        {
            output = new StringBuilder();
        }
    }

    public static void advance(ConsoleListener listener)
    {
        currentContext = listener.getContext();
        showOutput(listener);
    }

    private static class ShowDialogOnCloseScript implements EveryFrameScript
    {
        private final SectorEntityToken token;
        private final InteractionDialogPlugin dialog;
        private boolean isDone = false;

        private ShowDialogOnCloseScript(InteractionDialogPlugin dialog,
                                        SectorEntityToken token)
        {
            this.dialog = dialog;
            this.token = token;
        }

        @Override
        public boolean isDone()
        {
            return isDone;
        }

        @Override
        public boolean runWhilePaused()
        {
            return true;
        }

        @Override
        public void advance(float amount)
        {
            final CampaignUIAPI ui = Global.getSector().getCampaignUI();
            if (!isDone && !ui.isShowingDialog() && !ui.isShowingMenu())
            {
                isDone = true;

                try
                {
                    if (dialog == null)
                    {
                        ui.showInteractionDialog(token);
                    }
                    else
                    {
                        ui.showInteractionDialog(dialog, token);
                    }
                }
                // Catching the exception won't actually help
                // The game is screwed at this point, honestly
                catch (Exception ex)
                {
                    if (dialog != null)
                    {
                        Console.showException("Failed to open dialog "
                                + dialog.getClass().getCanonicalName(), ex);
                    }

                    Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
                }
            }
        }
    }

    private Console()
    {
    }
}
