package org.lazywizard.console;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.graphics.PositionAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Level;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.ConsoleSettings.KeyStroke;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class ConsoleCampaignListener implements EveryFrameScript, ConsoleListener
{
    private transient float timeUntilNotify = 0.5f;
    private transient CampaignPopup popup = null;
    private transient boolean isDialogOpen = false;

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return true;
    }

    private boolean checkInput()
    {
        KeyStroke key = Console.getSettings().getConsoleSummonKey();
        boolean modPressed = true;

        if (key.requiresShift() && !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)))
        {
            modPressed = false;
        }

        if (key.requiresControl() && !(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)))
        {
            modPressed = false;
        }

        if (key.requiresAlt() && !(Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                || Keyboard.isKeyDown(Keyboard.KEY_RMENU)))
        {
            modPressed = false;
        }

        return (modPressed && Keyboard.isKeyDown(key.getKey()));
    }

    @Override
    public void advance(float amount)
    {
        if (timeUntilNotify > 0f)
        {
            timeUntilNotify -= amount;
        }

        if (checkInput())
        {
            isDialogOpen = true;
            Global.getSector().getCampaignUI().showInteractionDialog(
                    new CampaignPopup(), Global.getSector().getPlayerFleet());
        }

        if (!isDialogOpen && popup != null)
        {
            popup = null;
        }

        Console.advance(amount, this);
    }

    @Override
    public boolean showOutput(String output)
    {
        if (timeUntilNotify > 0f)
        {
            return false;
        }

        if (isDialogOpen)
        {
            for (String message : output.split("\n"))
            {
                popup.getDialog().getTextPanel().addParagraph(message,
                        Console.getSettings().getOutputColor());
            }
        }
        else
        {
            for (String message : output.split("\n"))
            {
                Global.getSector().getCampaignUI().addMessage(message,
                        Console.getSettings().getOutputColor());
            }
        }

        return true;
    }

    @Override
    public CommandContext getContext()
    {
        return CommandContext.CAMPAIGN_MAP;
    }

    //<editor-fold defaultstate="collapsed" desc="Console popup">
    private class CampaignPopup implements InteractionDialogPlugin
    {
        private final Object LEAVE = new Object();
        private InteractionDialogAPI dialog;
        private KeyListener keyListener;
        private float timeOpen = 0f; // Used for the blinking cursor
        private int easterEggLevel = -1; // Set to <0 to disable easter eggs

        @Override
        public void init(InteractionDialogAPI dialog)
        {
            popup = this;
            this.dialog = dialog;
            keyListener = new KeyListener();
            timeOpen = 0f;

            final int width = Display.getWidth(), height = Display.getHeight();
            dialog.setTextWidth(width * .9f);
            dialog.setTextHeight(height * .8f);
            dialog.setXOffset(width * .45f);
            dialog.setYOffset(height * .05f);
            dialog.getVisualPanel().showCustomPanel(0f, 0f, keyListener);
            dialog.getTextPanel().addParagraph(CommonStrings.INPUT_QUERY);
            dialog.setPromptText("Input: ");

            dialog.getOptionPanel().addOption("Close", LEAVE);
            dialog.setOptionOnEscape("Close", LEAVE);
        }

        @Override
        public void optionSelected(String optionText, Object optionData)
        {
            if (optionData == LEAVE)
            {
                dialog.dismiss();
                isDialogOpen = false;
            }
        }

        @Override
        public void optionMousedOver(String optionText, Object optionData)
        {
        }

        @Override
        public void advance(float amount)
        {
            timeOpen += amount;
            final String input = keyListener.currentInput.toString();
            final String cursor = ((((int) timeOpen) & 1) == 0 ? "|" : " ");
            final boolean showIndex = Console.getSettings().getShouldShowCursorIndex();
            final int index = keyListener.currentIndex;
            if (index == input.length())
            {
                dialog.setPromptText("Input: " + input + cursor
                        + (showIndex ? " | Index: " + index + "/" + input.length() : ""));
            }
            else
            {
                dialog.setPromptText("Input: " + input.substring(0, index)
                        + cursor + input.substring(index, input.length())
                        + (showIndex ? " | Index: " + index + "/" + input.length() : ""));
            }

            // Easter eggs, because why not?
            if (easterEggLevel == 0 && timeOpen > 180f) // 3 minutes
            {
                dialog.getTextPanel().addParagraph("Take your time. I have all"
                        + " day.", Color.RED);
                easterEggLevel++;
            }
            else if (easterEggLevel == 1 && timeOpen > 300f) // 5 minutes
            {
                dialog.getTextPanel().addParagraph("I've heard that if you"
                        + " enter 'runcode System.exit(0)' magical things"
                        + " happen!", Color.RED);
                easterEggLevel++;
            }
            else if (easterEggLevel == 2 && timeOpen > 330f) // 5 1/2 minutes
            {
                dialog.getTextPanel().addParagraph("Okay, that was a lie. Can't"
                        + " fool you it seems.", Color.RED);
                easterEggLevel++;
            }
        }

        @Override
        public void backFromEngagement(EngagementResultAPI battleResult)
        {
        }

        @Override
        public Object getContext()
        {
            return null;
        }

        public InteractionDialogAPI getDialog()
        {
            return dialog;
        }

        @Override
        public Map<String, MemoryAPI> getMemoryMap()
        {
            return null;
        }

        // TODO: Add support for holding down keys
        private class KeyListener implements CustomUIPanelPlugin
        {
            final StringBuilder currentInput = new StringBuilder();
            String lastInput = null;
            int currentIndex = 0, lastIndex = 0;

            @Override
            public void positionChanged(PositionAPI position)
            {
            }

            @Override
            public void render(float alphaMult)
            {
            }

            @Override
            public void advance(float amount)
            {
            }

            @Override
            public void processInput(List<InputEventAPI> events)
            {
                final int previousLength = currentInput.length();

                try
                {
                    for (InputEventAPI event : events)
                    {
                        if (event.isConsumed() || !event.isKeyDownEvent()
                                || event.isModifierKey())
                        {
                            continue;
                        }

                        final int keyPressed = event.getEventValue();
                        timeOpen = 0f;

                        // Load last command when user presses up on keyboard
                        if (keyPressed == Keyboard.KEY_UP && Console.getLastCommand() != null)
                        {
                            lastInput = currentInput.toString();
                            lastIndex = currentIndex;
                            currentInput.replace(0, currentInput.length(),
                                    Console.getLastCommand());
                            currentIndex = currentInput.length();
                            event.consume();
                            continue;
                        }

                        // Down restores previous command overwritten by up
                        if (keyPressed == Keyboard.KEY_DOWN && lastInput != null)
                        {
                            currentInput.replace(0, currentInput.length(), lastInput);
                            currentIndex = lastIndex;
                            lastInput = null;
                            lastIndex = currentInput.length();
                            event.consume();
                            continue;
                        }

                        // Tab auto-completes the command
                        if (keyPressed == Keyboard.KEY_TAB)
                        {
                            // Only auto-complete if arguments haven't been entered
                            if (currentInput.indexOf(" ") != -1)
                            {
                                event.consume();
                                continue;
                            }

                            // Used for comparisons
                            final String toIndex = currentInput.substring(0, currentIndex),
                                    fullCommand = currentInput.toString();

                            // Cycle through matching commands from current index forward
                            // If no further matches are found, start again from beginning
                            String firstMatch = null, nextMatch = null;
                            final List<String> commands = CommandStore.getLoadedCommands();
                            Collections.sort(commands);
                            // Reverse order when shift is held down
                            if (event.isShiftDown())
                            {
                                Collections.reverse(commands);
                            }
                            for (String command : commands)
                            {
                                if (command.regionMatches(true, 0, toIndex, 0, toIndex.length()))
                                {
                                    // Used to cycle back to the beginning when no more matches are found
                                    if (firstMatch == null)
                                    {
                                        firstMatch = command;
                                    }

                                    // Found next matching command
                                    if ((event.isShiftDown() && command.compareToIgnoreCase(fullCommand) < 0)
                                            || (!event.isShiftDown() && command.compareToIgnoreCase(fullCommand) > 0))
                                    {
                                        nextMatch = command;
                                        break;
                                    }
                                }
                            }

                            if (nextMatch != null)
                            {
                                currentInput.replace(0, currentInput.length(), nextMatch);
                            }
                            else if (firstMatch != null)
                            {
                                currentInput.replace(0, currentInput.length(), firstMatch);
                            }

                            event.consume();
                            continue;
                        }

                        // Left or right move the editing cursor
                        if (keyPressed == Keyboard.KEY_LEFT)
                        {
                            currentIndex = Math.max(0, currentIndex - 1);
                            event.consume();
                            continue;
                        }
                        if (keyPressed == Keyboard.KEY_RIGHT)
                        {
                            currentIndex = Math.min(currentInput.length(),
                                    currentIndex + 1);
                            event.consume();
                            continue;
                        }

                        // Home = move cursor to beginning of line
                        // End = move cursor to end of line
                        if (keyPressed == Keyboard.KEY_HOME)
                        {
                            currentIndex = 0;
                            event.consume();
                            continue;
                        }
                        if (keyPressed == Keyboard.KEY_END)
                        {
                            currentIndex = currentInput.length();
                            event.consume();
                            continue;
                        }

                        // Backspace handling, imitates vanilla text inputs
                        if (keyPressed == Keyboard.KEY_BACK && currentIndex > 0)
                        {
                            // Control+backspace, delete last word
                            // TODO: Add positional editing support
                            if (event.isCtrlDown())
                            {
                                int lastSpace = currentInput.lastIndexOf(" ");
                                if (lastSpace == -1)
                                {
                                    currentInput.setLength(0);
                                }
                                else
                                {
                                    currentInput.delete(lastSpace, currentInput.length());
                                }
                            }
                            // Regular backspace, delete last character
                            else
                            {
                                currentInput.deleteCharAt(currentIndex - 1);
                            }

                            event.consume();
                        }
                        // Delete key handling
                        else if (keyPressed == Keyboard.KEY_DELETE
                                && currentIndex < currentInput.length())
                        {
                            currentInput.deleteCharAt(currentIndex);
                            currentIndex++;
                            event.consume();
                        }
                        // Return key handling
                        else if (keyPressed == Keyboard.KEY_RETURN)
                        {
                            String command = currentInput.toString();
                            Console.parseInput(command, CommandContext.CAMPAIGN_MAP);
                            currentInput.setLength(0);
                            currentIndex = 0;
                            lastInput = null;
                            lastIndex = 0;
                            event.consume();
                        }
                        // Paste handling
                        else if (keyPressed == Keyboard.KEY_V && event.isCtrlDown())
                        {
                            currentInput.insert(currentIndex,
                                    Sys.getClipboard().replace('\n', ' '));
                            event.consume();
                        }
                        // Normal typing
                        else
                        {
                            // TODO: add international character support
                            final char character = event.getEventChar();
                            if (character >= 0x20 && character <= 0x7e)
                            {
                                currentInput.insert(currentIndex, character);
                                event.consume();
                            }
                            else
                            {
                                continue;
                            }
                        }

                        // Update cursor index based on what changed since last input
                        currentIndex += currentInput.length() - previousLength;
                        currentIndex = Math.min(Math.max(0, currentIndex),
                                currentInput.length());
                    }
                }
                catch (ArrayIndexOutOfBoundsException ex)
                {
                    Console.showMessage("Something went wrong with the input parser!"
                            + "Please send a copy of starsector.log to LazyWizard.");
                    Global.getLogger(KeyListener.class).log(Level.ERROR,
                            "Input dump:\n - Current input: " + currentInput.toString()
                            + " | Index: " + currentIndex + "/" + currentInput.length()
                            + "\n - Last input: " + (lastInput == null ? "null"
                                    : lastInput + " | Index: "
                                    + lastIndex + "/" + lastInput.length()), ex);
                    currentIndex = currentInput.length();
                    lastIndex = 0;
                }
            }
        }
    }
    //</editor-fold>
}
