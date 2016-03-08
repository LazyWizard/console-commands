package org.lazywizard.console.util;

/**
 * Thrown when something has gone wrong while retrieving or loading a font.
 *
 * @author LazyWizard
 * @since 3.0
 */
public class FontException extends Exception
{
    public FontException(String message)
    {
        super(message);
    }

    public FontException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public FontException(Throwable cause)
    {
        super(cause);
    }
}
