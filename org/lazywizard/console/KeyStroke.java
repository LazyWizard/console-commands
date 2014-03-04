package org.lazywizard.console;

class KeyStroke
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
