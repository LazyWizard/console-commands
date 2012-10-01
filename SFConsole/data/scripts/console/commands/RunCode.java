package data.scripts.console.commands;

import data.scripts.console.BaseCommand;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.*;
//import org.lazywizard.helpers.CombinedClassLoader;

public class RunCode extends BaseCommand
{
    static transient ScriptEvaluator eval;
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
        eval.setParentClassLoader(ClassLoader.getSystemClassLoader());
        //eval.setParentClassLoader(new CombinedClassLoader());
        eval.setDefaultImports(new String[]
                {
                    "com.fs.starfarer.api.*", "java.util.*",
                    "com.fs.starfarer.api.campaign.*", "java.awt.Color",
                    "com.fs.starfarer.api.fleet.*", "data.scripts.*",
                    "data.scripts.world.*", "java.lang.Math"
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
    public boolean runCommand(String args)
    {
        // Yes, this is relatively slow. But for obvious security reasons,
        // I'd rather allow only manually-entered code to run
        if (!"data.scripts.console.Console".equals(
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
            showMessage("Compilation failed:",
                    ex.toString() + "\n" + ex.getMessage(), true);
            return false;
        }
        catch (InvocationTargetException ex)
        {
            showMessage("Execution failed:",
                    ex.toString() + "\n" + ex.getMessage(), true);
            return false;
        }

        return true;
    }
}
