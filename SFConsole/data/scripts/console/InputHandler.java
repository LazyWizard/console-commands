package data.scripts.console;

import org.lwjgl.input.Keyboard;

public class InputHandler extends Thread
{
    private static final long FRAMERATE = (long) (1000 / 20);
    private static int consoleKey = Keyboard.KEY_GRAVE;
    private static InputHandler instance = new InputHandler();
    private transient boolean isPressed = false;

    private InputHandler()
    {
    }

    public synchronized static InputHandler getInputHandler()
    {
        if (instance == null)
        {
            instance = new InputHandler();
        }

        return instance;
    }

    public synchronized static void setConsoleKey(int key)
    {
        consoleKey = key;
    }

    private void checkInput()
    {
        if (!isPressed)
        {
            if (Keyboard.isKeyDown(consoleKey))
            {
                isPressed = true;

                // Due to a bug with LWJGL input and window focus, the console
                // will only activate once the console key is released
            }
        }
        else
        {
            if (!Keyboard.isKeyDown(consoleKey))
            {
                isPressed = false;

                if (!ConsoleManager.allowConsole())
                {
                    ConsoleManager.showRestrictions();
                    return;
                }

                Console.getInput();
            }
        }
    }

    @Override
    public void run()
    {
        while (true)
        {
            checkInput();

            try
            {
                Thread.sleep(FRAMERATE);
            }
            catch (InterruptedException ex)
            {
                throw new RuntimeException("Console input thread interrupted!");
            }
        }
    }
}
