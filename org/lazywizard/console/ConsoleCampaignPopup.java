package org.lazywizard.console;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.graphics.PositionAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.awt.Color;
import java.util.List;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lwjgl.input.Keyboard;

class ConsoleCampaignPopup implements InteractionDialogPlugin
{
    private static final Object LEAVE = new Object();
    private InteractionDialogAPI dialog;
    private KeyListener keyListener;

    @Override
    public void init(InteractionDialogAPI dialog)
    {
        this.dialog = dialog;
        keyListener = new KeyListener();

        dialog.getVisualPanel().showCustomPanel(0f, 0f, keyListener);
        dialog.getTextPanel().addParagraph(
                "Enter a command, or 'help' for a list of valid commands.");
        dialog.setPromptText("Input: ");

        dialog.getOptionPanel().addOption("Cancel", LEAVE);
        dialog.setOptionOnEscape("Cancel", LEAVE);
        /*KeyStroke key = Console.getConsoleKey();
         dialog.getOptionPanel().setShortcut(LEAVE,
         key.getKey(), key.requiresControl(), key.requiresAlt(),
         key.requiresShift(), true);*/
    }

    @Override
    public void optionSelected(String optionText, Object optionData)
    {
        if (optionData == LEAVE)
        {
            dialog.dismiss();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData)
    {
    }

    @Override
    public void advance(float amount)
    {
        dialog.setPromptText("Input: " + keyListener.currentInput.toString());
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

    private class KeyListener implements CustomUIPanelPlugin
    {
        StringBuilder currentInput = new StringBuilder();

        public void clearInput()
        {
            currentInput.setLength(0);
        }

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
            // TODO: temporary test code; this needs cleanup badly
            for (InputEventAPI event : events)
            {
                if (event.isConsumed() || !event.isKeyDownEvent()
                        || event.isModifierKey())
                {
                    continue;
                }

                if (event.getEventValue() == Keyboard.KEY_BACK
                        && currentInput.length() > 0)
                {
                    currentInput.deleteCharAt(currentInput.length() - 1);
                    System.out.println(currentInput.toString() + ".");
                    event.consume();
                }
                else if (event.getEventValue() == Keyboard.KEY_RETURN)
                {
                    String command = currentInput.toString();
                    dialog.getTextPanel().addParagraph("Running command \""
                            + command + "\"", Color.CYAN);
                    Console.parseInput(command, CommandContext.CAMPAIGN_MAP);
                    currentInput.setLength(0);
                    event.consume();
                }
                else
                {
                    // Goodbye, international character support...
                    char character = event.getEventChar();
                    if (character >= 0x20 && character <= 0x7e)
                    {
                        currentInput.append(character);
                        event.consume();
                    }
                }
            }
        }
    }
}
