package org.lazywizard.console.util;

import java.lang.ref.Reference;
import java.lang.reflect.Field;

// Rough equivalent to C#'s GC.SuppressFinalize()
// Author: Luke Quinane, http://lukequinane.blogspot.com/2009/03/java-suppress-finalizer.html
public class FinalizeHelper
{
    private static final FinalizeHelper finalizeHelper = new FinalizeHelper();
    private final Class<?> finalizerClazz;
    private final Object lock;
    private final Field unfinalizedField, nextField, prevField, referentField;

    private FinalizeHelper()
    {
        try
        {
            finalizerClazz = Class.forName("java.lang.ref.Finalizer");

            // we need to lock on this field to avoid racing conditions
            Field lockField = finalizerClazz.getDeclaredField("lock");
            lockField.setAccessible(true);
            lock = lockField.get(null);

            // the start into the linked list of finalizers
            unfinalizedField = finalizerClazz.getDeclaredField("unfinalized");
            unfinalizedField.setAccessible(true);

            // the next element in the linked list
            nextField = finalizerClazz.getDeclaredField("next");
            nextField.setAccessible(true);

            // the prev element in the linked list
            prevField = finalizerClazz.getDeclaredField("prev");
            prevField.setAccessible(true);

            // the object that the finalizer is defined on
            referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);

        }
        catch (IllegalArgumentException | SecurityException ex)
        {
            throw ex;
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException ex)
        {
            throw new IllegalStateException("Could not create FinalizeHelper", ex);
        }
    }

    private void suppress(Object instance)
    {
        try
        {
            synchronized (lock)
            {
                // Get the start of the un-finalized list
                Object current = unfinalizedField.get(null);
                Object previous = null;

                while (current != null)
                {
                    Object value = referentField.get(current);

                    // Check if this entry refers to the instance we are interested in
                    if (value == instance)
                    {
                        // Unlink the current entry from the queue
                        Object next = nextField.get(current);
                        if (previous == null)
                        {
                            unfinalizedField.set(null, next);
                            prevField.set(next, null);
                        }
                        else
                        {
                            nextField.set(previous, next);
                            prevField.set(next, previous);
                        }
                        break;
                    }

                    // Move to the next entry
                    previous = current;
                    current = nextField.get(current);
                }
            }
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public static void suppressFinalize(Object instance)
    {
        finalizeHelper.suppress(instance);
    }
}
