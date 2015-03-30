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

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Nullable;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import resolvelite.codegen.CodeGenPipeline;
import resolvelite.compiler.tree.ImportCollection;
import resolvelite.compiler.tree.AnnotatedTree;
import resolvelite.compiler.tree.ResolveTokenFactory;
import resolvelite.misc.FileLocator;
import resolvelite.misc.LogManager;
import resolvelite.misc.Utils;
import resolvelite.parsing.ResolveLexer;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.AnalysisPipeline;
import resolvelite.semantics.SymbolTable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;

public class ResolveCompiler {

    public static String VERSION = "2.22.15a";

    public static enum OptionArgType {
        NONE, STRING
    } // NONE implies boolean

    public static final List<String> NATIVE_EXT = Collections
            .unmodifiableList(Arrays.asList("concept", "precis", "facility"));

    public static final List<String> NON_NATIVE_EXT = Collections
            .unmodifiableList(Arrays.asList("java"));

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
            new Option("longMessages", "-longMessages",
                    "show exception details on errors"),
            new Option("genCode", "-genCode", OptionArgType.STRING,
                    "generate code"),
            new Option("workspaceDir", "-workspaceDir", OptionArgType.STRING,
                    "specify root location of current workspace housing"
                            + "resolve files"),
            new Option("log", "-Xlog",
                    "dump lots of logging info to resolve-timestamp.log") };

    public final String[] args;

    public boolean helpFlag = false;
    public boolean longMessages = false;
    public String genCode;
    public String workspaceDir;
    public boolean log = false;

    public final DefaultCompilerListener defaultListener =
            new DefaultCompilerListener(this);
    public final SymbolTable symbolTable = new SymbolTable(this);

    public final List<String> targetFiles = new ArrayList<>();
    public final List<String> targetNames = new ArrayList<>();
    public final ErrorManager errorManager;
    public LogManager logMgr = new LogManager();

    public ResolveCompiler(String[] args) {
        this.errorManager = new ErrorManager(this);
        this.args = args;
        handleArgs();
    }

    public void handleArgs() {
        int i = 0;
        while (args != null && i < args.length) {
            String arg = args[i];
            i++;
            if ( arg.charAt(0) != '-' ) { // file name
                if ( !targetFiles.contains(arg) ) {
                    targetFiles.add(arg);
                    String f = Utils.groomFileName(arg);
                    targetNames.add(f.substring(0, f.indexOf(".")));
                }
                continue;
            }
            boolean found = false;
            for (Option o : optionDefs) {
                if ( arg.equals(o.name) ) {
                    found = true;
                    String argValue = null;
                    if ( o.argType == OptionArgType.STRING ) {
                        argValue = args[i];
                        i++;
                    }
                    // use reflection to set field
                    Class<? extends ResolveCompiler> c = this.getClass();
                    try {
                        Field f = c.getField(o.fieldName);
                        if ( argValue == null ) {
                            if ( arg.startsWith("-no-") )
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
            if ( !found ) {
                errorManager.toolError(ErrorKind.INVALID_CMDLINE_ARG, arg);
            }
        }
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
                    resolve.errorManager.toolError(ErrorKind.INTERNAL_ERROR,
                            ioe);
                }
            }
        }
        if ( resolve.errorManager.getErrorCount() > 0 ) {
            resolve.exit(1);
        }
        resolve.exit(0);
    }

    public void processCommandLineTargets() {
        List<AnnotatedTree> targets = sortTargetModulesByUsesReferences();
        int initialErrCt = errorManager.getErrorCount();
        AnalysisPipeline analysisPipe = new AnalysisPipeline(this, targets);
        CodeGenPipeline codegenPipe = new CodeGenPipeline(this, targets);

        analysisPipe.process();
        if ( analysisPipe.compiler.errorManager.getErrorCount() > initialErrCt ) {
            return;
        }
        codegenPipe.process();
    }

    public List<AnnotatedTree> sortTargetModulesByUsesReferences() {
        Map<String, AnnotatedTree> roots = new HashMap<>();
        for (String fileName : targetFiles) {
            AnnotatedTree t = parseModule(fileName);
            if ( t == null || t.hasErrors ) {
                continue;
            }
            roots.put(t.getName(), t);
        }
        DefaultDirectedGraph<String, DefaultEdge> g =
                new DefaultDirectedGraph<>(DefaultEdge.class);
        for (AnnotatedTree t : Collections.unmodifiableCollection(roots
                .values())) {
            g.addVertex(t.getName());
            findDependencies(g, t, roots);
        }
        List<AnnotatedTree> finalOrdering = new ArrayList<>();
        for (String s : getCompileOrder(g)) {
            AnnotatedTree m = roots.get(s);
            if ( m.hasErrors ) {
                finalOrdering.clear();
                break;
            }
            finalOrdering.add(m);
        }
        return finalOrdering;
    }

    private void findDependencies(DefaultDirectedGraph<String, DefaultEdge> g,
            AnnotatedTree root, Map<String, AnnotatedTree> roots) {
        for (String importRequest : root.imports
                .getImportsExcluding(ImportCollection.ImportType.EXTERNAL)) {
            AnnotatedTree module = roots.get(importRequest);
            try {
                File file = findResolveFile(importRequest, NATIVE_EXT);

                if ( module == null ) {
                    module = parseModule(file.getAbsolutePath());
                    roots.put(module.getName(), module);
                }
            }
            catch (IOException ioe) {
                errorManager.semanticError(ErrorKind.MISSING_IMPORT_FILE, null,
                        importRequest, importRequest);
                //mark the current root as erroneous
                root.hasErrors = true;
                continue;
            }

            if ( root.imports.inCategory(ImportCollection.ImportType.NAMED,
                    module.getName()) ) {
                /*
                 * if (!module.appropriateForImport()) {
                 * errorManager.toolError(ErrorKind.INVALID_IMPORT,
                 * "MODULE TYPE GOES HERE", root.getName().getText(),
                 * "IMPORTED MODULE TYPE GOES HERE", module.getName()
                 * .getText());
                 * }
                 */
            }
            if ( pathExists(g, module.getName(), root.getName()) ) {
                //Todo.
                throw new IllegalStateException("circular dependency detected");
            }
            Graphs.addEdgeWithVertices(g, root.getName(), module.getName());
            findDependencies(g, module, roots);
        }
    }

    protected List<String> getCompileOrder(
            DefaultDirectedGraph<String, DefaultEdge> g) {
        List<String> result = new ArrayList<>();

        EdgeReversedGraph<String, DefaultEdge> reversed =
                new EdgeReversedGraph<>(g);

        TopologicalOrderIterator<String, DefaultEdge> dependencies =
                new TopologicalOrderIterator<>(reversed);
        while (dependencies.hasNext()) {
            result.add(dependencies.next());
        }
        return result;
    }

    protected boolean pathExists(DefaultDirectedGraph<String, DefaultEdge> g,
            String src, String dest) {
        //If src doesn't exist in g, then there is obviously no path from
        //src -> ... -> dest
        if ( !g.containsVertex(src) ) {
            return false;
        }
        GraphIterator<String, DefaultEdge> iterator =
                new DepthFirstIterator<>(g, src);
        while (iterator.hasNext()) {
            String next = iterator.next();
            //we've reached dest from src -- a path exists.
            if ( next.equals(dest) ) {
                return true;
            }
        }
        return false;
    }

    private File findResolveFile(String baseName, List<String> extensions)
            throws IOException {
        FileLocator l = new FileLocator(baseName, extensions);
        Files.walkFileTree(new File(System.getProperty("user.dir")).toPath(), l);
        return l.getFile();
    }

    private AnnotatedTree parseModule(String fileName) {
        try {
            File file = new File(fileName);
            if ( !file.isAbsolute() ) {
                file = new File(System.getProperty("user.dir"), fileName);
            }
            ANTLRInputStream input =
                    new ANTLRFileStream(file.getAbsolutePath());
            ResolveLexer lexer = new ResolveLexer(input);
            ResolveTokenFactory factory = new ResolveTokenFactory(input);
            lexer.setTokenFactory(factory);

            TokenStream tokens = new CommonTokenStream(lexer);
            ResolveParser parser = new ResolveParser(tokens);
            parser.setTokenFactory(factory);
            parser.removeErrorListeners();
            parser.addErrorListener(errorManager);
            ParserRuleContext start = parser.module();

            AnnotatedTree result =
                    new AnnotatedTree(start, Utils.getModuleName(start),
                            parser.getSourceName());
            result.hasErrors = parser.getNumberOfSyntaxErrors() > 0;
            return result;
        }
        catch (IOException ioe) {
            errorManager.toolError(ErrorKind.CANNOT_OPEN_FILE, ioe, fileName);
        }
        return null;
    }

    public void log(@Nullable String component, String msg) {
        logMgr.log(component, msg);
    }

    public void log(String msg) {
        log(null, msg);
    }

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