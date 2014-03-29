/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lazywizard.console;

import java.awt.Color;

/**
 * Contains the console settings loaded from the JSON file.
 *
 * @author LazyWizard
 * @since 2.0
 */
// TODO: Javadoc this
public class ConsoleSettings
{
    // The key stroke that summons the console pop-up
    private final KeyStroke CONSOLE_SUMMON_KEY;
    // The String (usually a single character) that separates multiple commands
    private final String COMMAND_SEPARATOR;
    // The color of the console's output text
    private final Color OUTPUT_COLOR;
    // How many characters before the output is line-wrapped
    private final int OUTPUT_MAX_LINE_LENGTH;

    ConsoleSettings(KeyStroke consoleSummonKey, String commandSeparator,
            Color outputColor, int outputMaxLineLength)
    {
        CONSOLE_SUMMON_KEY = consoleSummonKey;
        COMMAND_SEPARATOR = commandSeparator;
        OUTPUT_COLOR = outputColor;
        OUTPUT_MAX_LINE_LENGTH = outputMaxLineLength;
    }

    public KeyStroke getConsoleSummonKey()
    {
        return CONSOLE_SUMMON_KEY;
    }

    public String getCommandSeparator()
    {
        return COMMAND_SEPARATOR;
    }

    public Color getOutputColor()
    {
        return OUTPUT_COLOR;
    }

    public int getMaxOutputLineLength()
    {
        return OUTPUT_MAX_LINE_LENGTH;
    }

    public static class KeyStroke
    {
        private final int key;
        private final boolean requireShift;
        private final boolean requireControl;
        private final boolean requireAlt;

        KeyStroke(int key, boolean requireShift, boolean requireControl,
                boolean requireAlt)
        {
            this.key = key;
            this.requireShift = requireShift;
            this.requireControl = requireControl;
            this.requireAlt = requireAlt;
        }

        public int getKey()
        {
            return key;
        }

        public boolean requiresShift()
        {
            return requireShift;
        }

        public boolean requiresControl()
        {
            return requireControl;
        }

        public boolean requiresAlt()
        {
            return requireAlt;
        }
    }
}
