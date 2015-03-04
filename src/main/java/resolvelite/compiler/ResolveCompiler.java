/*
 * [The "BSD license"]
 * Copyright (c) 2015 Clemson University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package resolvelite.compiler;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import resolvelite.misc.LogManager;
import resolvelite.parsing.ResolveLexer;
import resolvelite.parsing.ResolveParser;
import resolvelite.typeandpopulate.MathSymbolTableBuilder;
import resolvelite.typeandpopulate.PopulatingListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ResolveCompiler {

    public static String VERSION = "2.22.15a";

    public static enum OptionArgType {
        NONE, STRING
    } // NONE implies boolean

    public static class Option {

        String fieldName;
        String name;
        OptionArgType argType;
        String description;

        public Option(String fieldName, String name, String description) {
            this(fieldName, name, OptionArgType.NONE, description);
        }

        public Option(String fieldName, String name, OptionArgType argType,
                String description) {
            this.fieldName = fieldName;
            this.name = name;
            this.argType = argType;
            this.description = description;
        }
    }

    public static Option[] optionDefs =
            {
                    new Option("longMessages", "-longMessages",
                            "show exception details on errors"),
                    new Option("log", "-Xlog",
                            "dump lots of logging info to resolve-timestamp.log") };

    public final ErrorManager errorManager;
    public final String[] args;

    public boolean helpFlag = false;
    public boolean longMessages = false;
    public boolean log = false;

    public final DefaultCompilerListener defaultListener =
            new DefaultCompilerListener(this);
    public final MathSymbolTableBuilder symbolTable =
            new MathSymbolTableBuilder(this);
    public final List<String> targetFiles = new ArrayList<String>();

    public LogManager logMgr = new LogManager();

    public ResolveCompiler(String[] args) {
        this.errorManager = new ErrorManager(this);
        this.args = args;
        handleArgs();
    }

    public void handleArgs() {
        for (String arg : args) {
            if (!arg.startsWith("-")) {
                if (!targetFiles.contains(arg)) {
                    targetFiles.add(arg);
                }
                continue;
            }
            boolean found = false;
            int i = 0;
            for (Option o : optionDefs) {
                if (arg.equals(o.name)) {
                    found = true;
                    String argValue = null;
                    if (o.argType == OptionArgType.STRING) {
                        argValue = args[i];
                        i++;
                    }
                    // use reflection to set field
                    Class<? extends ResolveCompiler> c = this.getClass();
                    try {
                        Field f = c.getField(o.fieldName);
                        if (argValue == null) {
                            if (arg.startsWith("-no-"))
                                f.setBoolean(this, false);
                            else
                                f.setBoolean(this, true);
                        }
                        else
                            f.set(this, argValue);
                    }
                    catch (Exception e) {
                        errorManager.toolError(ErrorKind.INTERNAL_ERROR,
                                "can't access field " + o.fieldName);
                    }
                }
            }
            if (!found) {
                errorManager.toolError(ErrorKind.INVALID_CMDLINE_ARG, arg);
            }
        }
    }

    public static void main(String[] args) {
        ResolveCompiler resolve = new ResolveCompiler(args);
        if (args.length == 0) {
            resolve.help();
            resolve.exit(0);
        }
        resolve.version();
        try {
            resolve.processCommandLineTargets();
        }
        finally {
            if (resolve.log) {
                try {
                    String logname = resolve.logMgr.save();
                    System.out.println("wrote " + logname);
                }
                catch (IOException ioe) {
                    resolve.errorManager.toolError(ErrorKind.INTERNAL_ERROR,
                            ioe);
                }
            }
        }
        if (resolve.errorManager.getErrorCount() > 0) {
            resolve.exit(1);
        }
        resolve.exit(0);
    }

    public void processCommandLineTargets() {
        List<ParseTree> targets = getTrees();

        for (ParseTree tree : targets) {
            AnnotatedParseTree.TreeAnnotatingBuilder annotations =
                    new AnnotatedParseTree.TreeAnnotatingBuilder(tree);
            PopulatingListener populator =
                    new PopulatingListener(this, annotations, symbolTable);
            ParseTreeWalker.DEFAULT.walk(populator, tree);
            int i = 0;
        }
    }

    public List<ParseTree> getTrees() {
        List<ParseTree> roots = new ArrayList<ParseTree>();
        for (String fileName : targetFiles) {
            try {
                File file = new File(fileName);
                if (!file.isAbsolute()) {
                    file = new File(System.getProperty("user.dir"), fileName);
                }
                ANTLRFileStream input =
                        new ANTLRFileStream(file.getAbsolutePath());
                ResolveLexer lexer = new ResolveLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                ResolveParser parser = new ResolveParser(tokens);
                roots.add(parser.module());
            }
            catch (IOException ioe) {
                errorManager.toolError(ErrorKind.CANNOT_OPEN_FILE, ioe,
                        fileName);
            }
        }
        return roots;
    }

    public void log(String msg) {}

    public void version() {
        info("RESOLVE Compiler Version " + VERSION);
    }

    public void help() {
        version();
        for (Option o : optionDefs) {
            String name =
                    o.name + (o.argType != OptionArgType.NONE ? " ___" : "");
            String s = String.format(" %-19s %s", name, o.description);
            info(s);
        }
    }

    public void info(String msg) {
        defaultListener.info(msg);
    }

    public void error(ResolveMessage msg) {
        defaultListener.error(msg);
    }

    public void warning(ResolveMessage msg) {
        defaultListener.warning(msg);
    }

    public void exit(int e) {
        System.exit(e);
    }
}
