package org.lazywizard.console;

import java.awt.Color;
import java.util.Map;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lwjgl.input.Keyboard;

/**
 * Contains the console settings loaded from the JSON file.
 *
 * @author LazyWizard
 * @since 2.0
 */
// TODO: Implement persistent settings between updates
public class ConsoleSettings
{
    // The key stroke that summons the console overlay
    private final KeyStroke CONSOLE_SUMMON_KEY;
    // The BMFont used by the console's overlay
    private final String CONSOLE_FONT;
    // The String (usually a single character) that separates multiple commands
    private final String COMMAND_SEPARATOR;
    // Whether each command should be displayed to the player before executing
    private final boolean SHOW_ENTERED_COMMANDS;
    // Whether the current cursor index is displayed in the overlay
    private final boolean SHOW_CURSOR_INDEX;
    // Whether to show full exception stack traces or just the class/message
    private final boolean SHOW_EXCEPTION_TRACE;
    // How similar two strings must be for typo correction to decide they match
    private final double TYPO_CORRECTION_THRESHOLD;
    // The color of the console's output text
    private final Color OUTPUT_COLOR;

    ConsoleSettings(int consoleSummonKey, boolean requireShift, boolean requireControl,
            boolean requireAlt, String commandSeparator, boolean showEnteredCommands,
            boolean showCursorIndex, boolean showStackTrace, double typoCorrectionThreshold,
            Color outputColor, String consoleFont)
    {
        CONSOLE_SUMMON_KEY = new KeyStroke(consoleSummonKey, requireShift,
                requireControl, requireAlt);
        COMMAND_SEPARATOR = commandSeparator;
        SHOW_ENTERED_COMMANDS = showEnteredCommands;
        SHOW_CURSOR_INDEX = showCursorIndex;
        SHOW_EXCEPTION_TRACE = showStackTrace;
        TYPO_CORRECTION_THRESHOLD = typoCorrectionThreshold;
        OUTPUT_COLOR = outputColor;
        CONSOLE_FONT = consoleFont;
    }

    /**
     * Returns the name of the font used by the console overlay.
     *
     * @return The name of the font used by the console.
     *
     * @since 3.0
     */
    public String getFont()
    {
        return CONSOLE_FONT;
    }

    /**
     * Returns the key (and any modifier keys) that must be pressed to summon
     * the console.
     * <p>
     * @return The {@link KeyStroke} to summon the console.
     * <p>
     * @since 2.0
     */
    public KeyStroke getConsoleSummonKey()
    {
        return CONSOLE_SUMMON_KEY;
    }

    /**
     * Returns what character sequence the player must separate multiple
     * commands with.
     * <p>
     * @return The {@link String} (usually a single character) that separates
     *         multiple commands.
     * <p>
     * @since 2.0
     */
    public String getCommandSeparator()
    {
        return COMMAND_SEPARATOR;
    }

    /**
     * Returns whether each valid individual command should be displayed before
     * being run.
     * <p>
     * @return {@code true} if each command should be displayed to the player
     *         before executing, {@code false} otherwise.
     * <p>
     * @since 2.0
     */
    public boolean getShouldShowEnteredCommands()
    {
        return SHOW_ENTERED_COMMANDS;
    }

    /**
     * Returns whether the console overlay shows the current index (for input
     * debugging purposes).
     * <p>
     * @return {@code true} if the console overlay shows the cursor index,
     *         {@code false} otherwise.
     * <p>
     * @since 2.3
     */
    public boolean getShouldShowCursorIndex()
    {
        return SHOW_CURSOR_INDEX;
    }

    /**
     * Returns whether
     * {@link Console#showException(java.lang.String, java.lang.Throwable)}
     * shows the full exception trace.
     * <p>
     * @return {@code true} if full exception stack traces are shown with
     *         {@link Console#showException(java.lang.String, java.lang.Throwable)}, {@code false}
     *         otherwise.
     * <p>
     * @since 2.7
     */
    public boolean getShouldShowExceptionStackTraces()
    {
        return SHOW_EXCEPTION_TRACE;
    }

    /**
     * Returns the threshold for similarity between two strings before one is
     * considered a typo of the other.
     * <p>
     * @return How similar two {@link String}s must be for typo correction to
     *         consider them a match.
     * <p>
     * @since 2.2
     */
    public double getTypoCorrectionThreshold()
    {
        return TYPO_CORRECTION_THRESHOLD;
    }

    /**
     * Returns the {@link Color} of the console's output.
     * <p>
     * @return The {@link Color} of the console's output.
     * <p>
     * @since 2.0
     */
    public Color getOutputColor()
    {
        return OUTPUT_COLOR;
    }

    /**
     * Represents the keys that must be pressed to summon the console.
     * <p>
     * @since 2.0
     */
    public static class KeyStroke
    {
        private final int key;
        private final boolean requireShift;
        private final boolean requireControl;
        private final boolean requireAlt;

        private KeyStroke(int key, boolean requireShift, boolean requireControl,
                boolean requireAlt)
        {
            this.key = key;
            this.requireShift = requireShift;
            this.requireControl = requireControl;
            this.requireAlt = requireAlt;
        }

        /**
         * Returns the key that must be pressed to summon the console.
         * <p>
         * The list of valid key codes can be found here:
         * <a
         * href=http://www.lwjgl.org/javadoc/constant-values.html#org.lwjgl.input.Keyboard.KEY_0>
         * http://www.lwjgl.org/javadoc/constant-values.html#org.lwjgl.input.Keyboard.KEY_0</a>
         * <p>
         * @return The LWJGL constant of the key that summons the console.
         * <p>
         * @since 2.0
         */
        public int getKey()
        {
            return key;
        }

        /**
         * Returns whether shift must be held down to summon the console.
         * <p>
         * @return Whether you must hold down shift while summoning the console.
         * <p>
         * @since 2.0
         */
        public boolean requiresShift()
        {
            return requireShift;
        }

        /**
         * Returns whether control must be held down to summon the console.
         * <p>
         * @return Whether you must hold down control while summoning the
         *         console.
         * <p>
         * @since 2.0
         */
        public boolean requiresControl()
        {
            return requireControl;
        }

        /**
         * Returns whether alt must be held down to summon the console.
         * <p>
         * @return Whether you must hold down alt while summoning the console.
         * <p>
         * @since 2.0
         */
        public boolean requiresAlt()
        {
            return requireAlt;
        }

        @Override
        public String toString()
        {
            String str = (key == Keyboard.KEY_BACK ? "BACKSPACE"
                    : Keyboard.getKeyName(key).toUpperCase());

            if (requireShift)
            {
                str = "SHIFT+" + str;
            }

            if (requireAlt)
            {
                str = "ALT+" + str;
            }

            if (requireControl)
            {
                str = "CONTROL+" + str;
            }

            return str;
        }
    }

    @Override
    public String toString()
    {
        return "ConsoleSettings{" + "CONSOLE_SUMMON_KEY=" + CONSOLE_SUMMON_KEY
                + ", COMMAND_SEPARATOR=" + COMMAND_SEPARATOR
                + ", SHOW_ENTERED_COMMANDS=" + SHOW_ENTERED_COMMANDS
                + ", SHOW_CURSOR_INDEX=" + SHOW_CURSOR_INDEX
                + ", TYPO_CORRECTION_THRESHOLD=" + TYPO_CORRECTION_THRESHOLD
                + ", OUTPUT_COLOR=" + OUTPUT_COLOR + '}';
    }
}
