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
package edu.clemson.resolve.compiler;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Nullable;

import edu.clemson.resolve.compiler.tree.ImportCollection;
import edu.clemson.resolve.compiler.tree.AnnotatedTree;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.misc.LogManager;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.parser.ResolveParser;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;

public class ResolveCompiler {

    public static String VERSION = "2.22.15a";

    public static enum OptionArgType {
        NONE, STRING
    } // NONE implies boolean

    public static final List<String> NATIVE_EXT = Collections
            .unmodifiableList(Collections.singletonList("resolve"));

    public static final List<String> NON_NATIVE_EXT = Collections
            .unmodifiableList(Collections.singletonList("java"));

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

    public static Option[] optionDefs = {
        new Option("longMessages",      "-longMessages", "show exception details on errors"),
        new Option("outputDirectory",   "-o", OptionArgType.STRING, "specify output directory where all output is generated"),
        new Option("longMessages",      "-long-messages", "show exception details when available for errors and warnings"),
        new Option("libDirectory",      "-lib", OptionArgType.STRING, "specify location of resolve source files"),
        new Option("genCode",           "-genCode", OptionArgType.STRING, "generate code"),
        new Option("vcs",               "-vcs", "generate verification conditions (VCs)"),
        new Option("log",               "-Xlog", "dump lots of logging info to resolve-timestamp.log")
    };

    public final String[] args;
    protected boolean haveOutputDir = false;

    public String libDirectory;
    public String outputDirectory;
    public boolean helpFlag = false;
    public boolean vcs = false;
    public boolean longMessages = false;
    public String genCode;
    public String workspaceDir;
    public boolean log = false;

    public final DefaultCompilerListener defaultListener =
            new DefaultCompilerListener(this);
    //public final SymbolTable symbolTable = new SymbolTable(this);

    public final List<String> targetFiles = new ArrayList<>();
    public final List<String> targetNames = new ArrayList<>();
    public final ErrorManager errMgr;
    public LogManager logMgr = new LogManager();

    public ResolveCompiler(String[] args) {
        this.errMgr = new ErrorManager(this);
        this.args = args;
        handleArgs();
    }

    public void handleArgs() {

    }

    public static void main(String[] args) {

        ResolveCompiler resolve = new ResolveCompiler(args);
        if ( args.length == 0 ) {
            resolve.help();
            resolve.exit(0);
        }
        resolve.version();
        try {
            resolve.processCommandLineTargets();
        }
        finally {
            if ( resolve.log ) {
                try {
                    String logname = resolve.logMgr.save();
                    System.out.println("wrote " + logname);
                }
                catch (IOException ioe) {
                    resolve.errMgr.toolError(ErrorKind.INTERNAL_ERROR,
                            ioe);
                }
            }
        }
        if ( resolve.errMgr.getErrorCount() > 0 ) {
            resolve.exit(1);
        }
        resolve.exit(0);
    }

    public void processCommandLineTargets() {

    }

    public void log(@Nullable String component, String msg) {
        logMgr.log(component, msg);
    }

    public void log(String msg) {
        log(null, msg);
    }

    public void version() {
        info("Resolve Compiler Version " + VERSION);
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