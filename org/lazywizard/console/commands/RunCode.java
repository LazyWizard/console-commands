package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ScriptEvaluator;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.Console;

public class RunCode implements BaseCommand
{
    private static ScriptEvaluator eval;

    private static void createEval()
    {
        eval = new ScriptEvaluator();
        eval.setReturnType(void.class);
        eval.setParentClassLoader(Global.getSettings().getScriptClassLoader());
        eval.setDefaultImports(new String[]
                {
                    "com.fs.starfarer.api.*", "java.util.*",
                    "com.fs.starfarer.api.campaign.*", "java.awt.Color",
                    "com.fs.starfarer.api.fleet.*", "data.scripts.*",
                    "com.fs.starfarer.api.combat.*", "data.scripts.world.*",
                    "org.lazywizard.lazylib.*", "org.lazywizard.lazylib.combat.*",
                    "org.lazywizard.lazylib.campaign.*", "org.lwjgl.util.vector.Vector2f"
                });
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        // Yes, this is relatively slow. But for obvious security reasons,
        // I'd rather allow only manually-entered code to run
        if (!Console.class.getCanonicalName().equals(
                new Throwable().getStackTrace()[1].getClassName()))
        {
            Console.showMessage("A mod attempted to execute arbitrary code on your machine!");
            return CommandResult.ERROR;
        }

        if (!args.endsWith(";"))
        {
            args = args + ";";
        }

        if (eval == null)
        {
            createEval();
        }

        try
        {
            eval.cook(args);
            eval.evaluate(null);
        }
        catch (CompileException ex)
        {
            Console.showException("Compilation failed: ", ex);
            return CommandResult.ERROR;
        }
        catch (InvocationTargetException ex)
        {
            Console.showException("Execution failed: ", ex.getTargetException());
            return CommandResult.ERROR;
        }

        return CommandResult.SUCCESS;
    }
}
