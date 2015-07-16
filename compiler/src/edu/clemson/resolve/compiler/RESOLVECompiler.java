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

import edu.clemson.resolve.codegen.CodeGenPipeline;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.misc.LogManager;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.analysis.AnalysisPipeline;
import edu.clemson.resolve.vcgen.VCGenPipeline;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Nullable;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.rsrg.semantics.SymbolTable;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public  class RESOLVECompiler {

    public static String VERSION = "0.0.1";

    public static final String FILE_EXTENSION = ".resolve";

    public static final List<String> NATIVE_EXTENSION = Collections
            .unmodifiableList(Collections.singletonList(FILE_EXTENSION));
    public static final List<String> NON_NATIVE_EXTENSION = Collections
            .unmodifiableList(Collections.singletonList(".java"));

    public static enum OptionArgType { NONE, STRING } // NONE implies boolean
    public static class Option {
        String fieldName;
        String name;
        OptionArgType argType;
        String description;

        public Option(String fieldName, String name, String description) {
            this(fieldName, name, OptionArgType.NONE, description);
        }
        public Option(String fieldName, String name, OptionArgType argType, String description) {
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
            new Option("noStdUses",         "-noStdUses", "don't import standard facilities (e.g. boolean, integer, char)"),
            new Option("genCode",           "-genCode", OptionArgType.STRING, "generate code"),
            new Option("vcs",               "-vcs", "generate verification conditions (VCs)"),
            new Option("log",               "-Xlog", "dump lots of logging info to edu.clemson.resolve-timestamp.log")
    };

    List<RESOLVECompilerListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Track separately so if someone adds a listener, it's the only one
     * instead of it and the default stderr listener.
     */
    DefaultCompilerListener defaultListener = new DefaultCompilerListener(this);
    public final SymbolTable symbolTable = new SymbolTable(this);

    public final String[] args;
    protected boolean haveOutputDir = false;
    public boolean noStdUses = false;
    public String libDirectory;
    public String outputDirectory;
    public boolean helpFlag = false;
    public boolean vcs = false;
    public boolean longMessages = false;
    public String genCode;
    public String workspaceDir;
    public boolean log = false;

    public final List<String> targetFiles = new ArrayList<>();
    public final List<String> targetNames = new ArrayList<>();
    public final ErrorManager errMgr;
    public LogManager logMgr = new LogManager();

    public RESOLVECompiler() { this(null); }

    public RESOLVECompiler(String[] args) {
        this.errMgr = new ErrorManager(this);
        this.args = args;
        handleArgs();
    }

    public void handleArgs() {
        int i = 0;
        while (args != null && i < args.length) {
            String arg = args[i];
            i++;
            if (arg.charAt(0) != '-') { // file name
                if (!targetFiles.contains(arg)) {
                    targetFiles.add(arg);
                    String name = Utils.groomFileName(arg);
                    int dotIdx = name.indexOf(".");
                    if (dotIdx != -1) {
                        name = name.substring(0, dotIdx);
                    }
                    targetNames.add(name);
                }
                continue;
            }
            boolean found = false;
            for (Option o : optionDefs) {
                if (arg.equals(o.name)) {
                    found = true;
                    String argValue = null;
                    if (o.argType == OptionArgType.STRING) {
                        argValue = args[i];
                        i++;
                    }
                    // use reflection to set field
                    Class<? extends RESOLVECompiler> c = this.getClass();
                    try {
                        Field f = c.getField(o.fieldName);
                        if (argValue == null) {
                            if (arg.startsWith("-no-"))
                                f.setBoolean(this, false);
                            else
                                f.setBoolean(this, true);
                        } else
                            f.set(this, argValue);
                    } catch (Exception e) {
                        errMgr.toolError(ErrorKind.INTERNAL_ERROR,
                                "can't access field " + o.fieldName);
                    }
                }
            }
            if (!found) {
                errMgr.toolError(ErrorKind.INVALID_CMDLINE_ARG, arg);
            }
        }
        if ( outputDirectory != null ) {
            if ( outputDirectory.endsWith("/")
                    || outputDirectory.endsWith("\\") ) {
                outputDirectory =
                        outputDirectory.substring(0,
                                outputDirectory.length() - 1);
            }
            File outDir = new File(outputDirectory);
            haveOutputDir = true;
            if ( outDir.exists() && !outDir.isDirectory() ) {
                errMgr.toolError(ErrorKind.OUTPUT_DIR_IS_FILE,
                        outputDirectory);
                libDirectory = ".";
            }
        }
        else {
            outputDirectory = ".";
        }
        if ( libDirectory != null ) {
            if ( libDirectory.endsWith("/") || libDirectory.endsWith("\\") ) {
                libDirectory =
                        libDirectory
                                .substring(0, libDirectory.length() - 1);
            }
            File outDir = new File(libDirectory);
            if ( !outDir.exists() ) {
                errMgr.toolError(ErrorKind.DIR_NOT_FOUND,
                        libDirectory);
                libDirectory = ".";
            }
        }
        else {
            libDirectory = ".";
        }
    }

    public static void main(String[] args) {
        RESOLVECompiler resolve = new RESOLVECompiler(args);
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
                    resolve.errMgr.toolError(ErrorKind.INTERNAL_ERROR, ioe);
                }
            }
        }
        if ( resolve.errMgr.getErrorCount() > 0 ) {
            resolve.exit(1);
        }
        resolve.exit(0);
    }

    public void processCommandLineTargets() {
        List<AnnotatedTree> targets = sortTargetModulesByUsesReferences();
        int initialErrCt = errMgr.getErrorCount();
        AnalysisPipeline analysisPipe = new AnalysisPipeline(this, targets);
        CodeGenPipeline codegenPipe = new CodeGenPipeline(this, targets);
        VCGenPipeline vcsPipe = new VCGenPipeline(this, targets);

        analysisPipe.process();
        if ( errMgr.getErrorCount() > initialErrCt ) {
            return;
        }
        codegenPipe.process();
        vcsPipe.process();
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

        for (AnnotatedTree t : Collections.unmodifiableCollection(roots.values())) {
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
                                  AnnotatedTree root,
                                  Map<String, AnnotatedTree> roots) {
        for (AnnotatedTree.UsesRef importRequest : root.uses) {
            AnnotatedTree module = roots.get(importRequest.name);
            try {
                File file = findResolveFile(importRequest.name, NATIVE_EXTENSION);
                if ( module == null ) {
                    module = parseModule(file.getAbsolutePath());
                    roots.put(module.getName(), module);
                }
            }
            catch (IOException ioe) {
                errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE,
                        importRequest.location, root.getName(),
                        importRequest.name);
                //mark the current root as erroneous
                root.hasErrors = true;
                continue;
            }

            if ( pathExists(g, module.getName(), root.getName()) ) {
                errMgr.semanticError(ErrorKind.CIRCULAR_DEPENDENCY,
                        importRequest.location,
                        importRequest.name, root.getName());
                break;
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

    private File findResolveFile(String fileName,
                        List<String> extensions) throws IOException {
        FileLocator l = new FileLocator(fileName, extensions);
        Files.walkFileTree(new File(libDirectory).toPath(), l);
        return l.getFile();
    }

    private AnnotatedTree parseModule(String fileName) {
        try {
            File file = new File(fileName);
            if ( !file.isAbsolute() ) {
                file = new File(libDirectory, fileName);
            }
            ANTLRInputStream input =
                    new ANTLRFileStream(file.getAbsolutePath());
            ResolveLexer lexer = new ResolveLexer(input);

            TokenStream tokens = new CommonTokenStream(lexer);
            Resolve parser = new Resolve(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(errMgr);
            ParserRuleContext start = parser.module();
            return new AnnotatedTree(start, Utils.getModuleName(start),
                    parser.getSourceName(),
                    parser.getNumberOfSyntaxErrors() > 0);
        }
        catch (IOException ioe) {
            errMgr.toolError(ErrorKind.CANNOT_OPEN_FILE, ioe, fileName);
        }
        return null;
    }

    /**
     * This method is used by all code generators to create new output
     * files. If the outputDir set by -o is not present it will be created.
     * The final filename is sensitive to the output directory and
     * the directory where the grammar file was found. If -o is /tmp
     * and the original resolve file was foo/T.resolve then output files
     * go in /tmp/foo.
     *
     * The output dir -o spec takes precedence if it's absolute.
     * E.g., if the grammar file dir is absolute the output dir is given
     * precendence. "-o /tmp /usr/lib/T.resolve" results in "/tmp/T.java" as
     * output (assuming T.resolve holds T.java).
     *
     * If no -o is specified, then just write to the directory where the
     * grammar file was found.
     *
     * If outputDirectory==null then write a String.
     */
    public Writer getOutputFileWriter(AnnotatedTree t, String fileName)
            throws IOException {
        if ( outputDirectory == null ) {
            return new StringWriter();
        }
        // output directory is a function of where the file lives
        // for subdir/T.resolve, you get subdir here.  Well, depends on -o etc...
        File outputDir = getOutputDirectory(t.getFileName());
        File outputFile = new File(outputDir, fileName);

        if ( !outputDir.exists() ) {
            outputDir.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        OutputStreamWriter osw;
        osw = new OutputStreamWriter(fos);

        return new BufferedWriter(osw);
    }

    /**
     * Return the location where the compiler will generate output files for a
     * given file. This is a base directory and output files will be relative to
     * here in some cases such as when -o option is used and input files are
     * given relative to the input directory.
     *
     * @param fileNameWithPath path to input source
     */
    public File getOutputDirectory(String fileNameWithPath) {
        File outputDir;
        String fileDirectory;

        // Some files are given to us without a PATH but should should
        // still be written to the output directory in the relative path of
        // the output directory. The file directory is either the set of sub directories
        // or just or the relative path recorded for the parent file.
        if ( fileNameWithPath.lastIndexOf(File.separatorChar) == -1 ) {
            // No path is included in the file name, so make the file
            // directory the same as the parent module (which might sitll be just ""
            // but when it is not, we will write the file in the correct place.
            fileDirectory = ".";

        }
        else {
            fileDirectory =
                    fileNameWithPath.substring(0,
                            fileNameWithPath.lastIndexOf(File.separatorChar));
        }
        if ( haveOutputDir ) {
            // -o /tmp /var/lib/T.resolve => /tmp/T.java
            // -o subdir/output /usr/lib/T.resolve => subdir/output/T.java
            // -o . /usr/lib/T.resolve => ./T.java
            if ( fileDirectory != null
                    && (new File(fileDirectory).isAbsolute() || fileDirectory
                    .startsWith("~")) ) { // isAbsolute doesn't count this :(
                // somebody set the dir, it takes precendence; write new file there
                outputDir = new File(outputDirectory);
            }
            else {
                // -o /tmp subdir/t.resolve => /tmp/subdir/t.resolve
                if ( fileDirectory != null ) {
                    outputDir = new File(outputDirectory, fileDirectory);
                }
                else {
                    outputDir = new File(outputDirectory);
                }
            }
        }
        else {
            // they didn't specify a -o dir so just write to location
            // where module is, absolute or relative, this will only happen
            // with command line invocation as build tools will always
            // supply an output directory.
            outputDir = new File(fileDirectory);
        }
        return outputDir;
    }

    public void log(@Nullable String component, String msg) {
        logMgr.log(component, msg);
    }

    public void log(String msg) {
        log(null, msg);
    }

    public void addListener(RESOLVECompilerListener cl) {
        if ( cl!=null ) listeners.add(cl);
    }

    public void removeListener(RESOLVECompilerListener tl) {
        listeners.remove(tl);
    }

    public void removeListeners() {
        listeners.clear();
    }

    public List<RESOLVECompilerListener> getListeners() {
        return listeners;
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
        if ( listeners.isEmpty() ) {
            defaultListener.info(msg);
            return;
        }
        for (RESOLVECompilerListener l : listeners) l.info(msg);
    }

    public void error(RESOLVEMessage msg) {
        if ( listeners.isEmpty() ) {
            defaultListener.error(msg);
            return;
        }
        for (RESOLVECompilerListener l : listeners) l.error(msg);
    }

    public void warning(RESOLVEMessage msg) {
        if ( listeners.isEmpty() ) {
            defaultListener.warning(msg);
        }
        else {
            for (RESOLVECompilerListener l : listeners) l.warning(msg);
        }
    }

    public void exit(int e) {
        System.exit(e);
    }
}
