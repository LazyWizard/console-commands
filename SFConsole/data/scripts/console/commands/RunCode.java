package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.console.BaseCommand;
import data.scripts.console.Console;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ScriptEvaluator;

public class RunCode extends BaseCommand
{
    private static ScriptEvaluator eval;
    // Most of these are to test classloader blocking
    /*static ExpressionEvaluator eval2;
     static ClassLoader tmp;
     static ClassBodyEvaluator eval3;
     static org.codehaus.janino.Compiler comp;
     static CompilerFactory comf;*/

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
    protected String getHelp()
    {
        return "Compiles and runs a line of code. This command has access to"
                + " most campaign API methods. Does not accept return statements.";
    }

    @Override
    protected String getSyntax()
    {
        return "runcode <line of code>";
    }

    @Override
    protected boolean isUseableInCombat()
    {
        return true;
    }

    @Override
    public boolean runCommand(String args)
    {
        // Yes, this is relatively slow. But for obvious security reasons,
        // I'd rather allow only manually-entered code to run
        if (!Console.class.getCanonicalName().equals(
                new Throwable().getStackTrace()[1].getClassName()))
        {
            showMessage("A mod attempted to execute arbitrary code on your machine!");
            return false;
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
            showError("Compilation failed:", ex);
            return false;
        }
        catch (InvocationTargetException ex)
        {
            showError("Execution failed:", ex.getTargetException());
            return false;
        }

        return true;
    }
}
