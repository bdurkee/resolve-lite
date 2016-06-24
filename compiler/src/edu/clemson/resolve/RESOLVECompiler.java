package edu.clemson.resolve;

import edu.clemson.resolve.codegen.CodeGenPipeline;
import edu.clemson.resolve.compiler.*;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.misc.LogManager;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.analysis.AnalysisPipeline;
import edu.clemson.resolve.vcgen.VerifierPipeline;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import edu.clemson.resolve.semantics.MathSymbolTable;
import edu.clemson.resolve.semantics.ModuleIdentifier;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * The main entrypoint for the compiler. All input flows into here and this is also where we manage flags for
 * commandline args which are encapsulated via instances of the {@link Option} class (which also resides here).
 * <p>
 * The structure and much of the code appearing here has been adapted to our prototype needs from the frontend of the
 * ANTLRv4 tool, publically available here: {@code https://github.com/antlr/antlr4}.</p>
 *
 * @since 0.0.1
 */
public class RESOLVECompiler {

    public static String VERSION = "0.0.1";

    public static final String NATIVE_FILE_EXTENSION = ".resolve";
    public static final String NON_NATIVE_FILE_EXTENSION = ".java";

    public static final List<String> ALL_FILE_EXTENSIONS =
            Collections.unmodifiableList(Arrays.asList(NATIVE_FILE_EXTENSION, NON_NATIVE_FILE_EXTENSION));

    private static enum OptionArgType {NONE, STRING} // NONE implies boolean

    private static class Option {
        String fieldName;
        String name;
        OptionArgType argType;
        String description;

        Option(@NotNull String fieldName, @NotNull String name, @NotNull String description) {
            this(fieldName, name, OptionArgType.NONE, description);
        }

        Option(@NotNull String fieldName, @NotNull String name, @NotNull OptionArgType argType, @NotNull String desc) {
            this.fieldName = fieldName;
            this.name = name;
            this.argType = argType;
            this.description = desc;
        }
    }

    //fields set by option manager
    public final String[] args;
    protected boolean haveOutputDir = false;

    //TODO: Change package of this to pkgLibDirectory or libDirectory.. something like that...
    public String libDirectory;
    public String outputDirectory;
    public boolean helpFlag = false;
    public boolean vcs = false;
    public boolean longMessages = false;
    public boolean prove = false;
    public String genCode;

    public boolean log = false;
    public boolean printEnv = false;

    public static Option[] optionDefs = {
            new Option("outputDirectory", "-o", OptionArgType.STRING, "specify output directory where all output is generated"),
            new Option("longMessages", "-long-messages", "show exception details when available for errors and warnings"),
            new Option("libDirectory", "-lib", OptionArgType.STRING, "specify location of resolve source files"),
            new Option("genCode", "-genCode", OptionArgType.STRING, "generate code"),
            new Option("genPackage", "-package", OptionArgType.STRING, "specify a package/namespace for the generated code"),
            new Option("vcs", "-vcs", "generate verification conditions (VCs)"),
            new Option("prove", "-prove", "attempt to prove generated VCs for the current file"),
            new Option("log", "-Xlog", "dump lots of logging info to edu.clemson.resolve-timestamp.log"),
            new Option("printEnv", "-env", "print path variables"),
    };

    List<RESOLVECompilerListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Track separately so if a listener is added, it's the only one (instead of it plus the default stderr listener).
     */
    DefaultCompilerListener defaultListener = new DefaultCompilerListener(this);
    public final MathSymbolTable symbolTable = new MathSymbolTable();

    public final List<String> targetFiles = new ArrayList<>();
    public final List<String> targetNames = new ArrayList<>();
    @NotNull
    public final ErrorManager errMgr;
    @NotNull
    public LogManager logMgr = new LogManager();

    /**
     * So if the user specifies on cmdline "compile T.resolve X.resolve " this will store <em>just</em> the
     * {@link AnnotatedModule}s for T and X (as opposed to say, {T, X} U {dependent modules}).
     */
    @NotNull
    public List<AnnotatedModule> commandlineTargets = new ArrayList<>();

    public RESOLVECompiler() {
        this(null);
    }

    public RESOLVECompiler(@Nullable String[] args) {
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
                if (!arg.endsWith(NATIVE_FILE_EXTENSION)) {
                    errMgr.toolError(ErrorKind.CANNOT_OPEN_FILE, arg);
                    continue;
                }
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
                            if (arg.startsWith("-no-")) {
                                f.setBoolean(this, false);
                            }
                            else {
                                f.setBoolean(this, true);
                            }
                        }
                        else
                            f.set(this, argValue);
                    } catch (Exception e) {
                        errMgr.toolError(ErrorKind.INTERNAL_ERROR, "can't access field " + o.fieldName);
                    }
                }
            }
            if (!found) {
                errMgr.toolError(ErrorKind.INVALID_CMDLINE_ARG, arg);
            }
        }
        if (outputDirectory != null) {
            if (outputDirectory.endsWith("/") || outputDirectory.endsWith("\\")) {
                outputDirectory = outputDirectory.substring(0, outputDirectory.length() - 1);
            }
            File outDir = new File(outputDirectory);
            haveOutputDir = true;
            if (outDir.exists() && !outDir.isDirectory()) {
                errMgr.toolError(ErrorKind.OUTPUT_DIR_IS_FILE, outputDirectory);
                libDirectory = ".";
            }
        }
        else {
            outputDirectory = ".";
        }
        if (libDirectory != null) {
            if (libDirectory.endsWith("/") || libDirectory.endsWith("\\")) {
                libDirectory = libDirectory.substring(0, libDirectory.length() - 1);
            }
            File outDir = new File(libDirectory);
            if (!outDir.exists()) {
                errMgr.toolError(ErrorKind.DIR_NOT_FOUND, libDirectory);
                libDirectory = ".";
            }
        }
        else {
            libDirectory = ".";
        }
    }

    public static void main(String[] args) {
        RESOLVECompiler resolve = new RESOLVECompiler(args);
        if (args.length == 0) {
            resolve.help();
            resolve.exit(0);
        }
        resolve.version();
        try {
            resolve.processCommandLineTargets();
        } finally {
            if (resolve.log) {
                try {
                    String logname = resolve.logMgr.save();
                    System.out.println("wrote " + logname);
                } catch (IOException ioe) {
                    resolve.errMgr.toolError(ErrorKind.INTERNAL_ERROR, ioe);
                }
            }
        }
        if (resolve.errMgr.getErrorCount() > 0) {
            resolve.exit(1);
        }
        resolve.exit(0);
    }

    public void processCommandLineTargets() {
        //TESTING CMDS:
        if (printEnv) {
            info("SYSTEM DIR: " + System.getProperty("user.dir"));
            info("HERE's new File('.'): " + new File(".").getAbsolutePath());
            info("$RESOLVEROOT=" + System.getenv("RESOLVEROOT"));
            Map<String, String> x = System.getenv();
            for (String o : x.keySet()) {
                info("key=" + o + ", " + "value=" + x.get(o));
            }
            info("core lib directory @: " + getCoreLibraryDirectory());
        }
        commandlineTargets.addAll(parseAndReturnRootModules());
        List<AnnotatedModule> targets = sortTargetModulesByUsesReferences(commandlineTargets);
        processCommandLineTargets(targets);
    }

    private List<AnnotatedModule> parseAndReturnRootModules() {
        List<AnnotatedModule> modules = new ArrayList<>();
        for (String e : targetFiles) {
            AnnotatedModule m = parseModule(e);
            if (m != null) {
                modules.add(m);
            }
        }
        return modules;
    }

    public void processCommandLineTargets(AnnotatedModule... module) {
        processCommandLineTargets(sortTargetModulesByUsesReferences(module));
    }

    public void processCommandLineTargets(List<AnnotatedModule> modules) {
        int initialErrCt = errMgr.getErrorCount();
        AnalysisPipeline analysisPipe = new AnalysisPipeline(this, modules);
        CodeGenPipeline codegenPipe = new CodeGenPipeline(this, modules);
        VerifierPipeline vcsPipe = new VerifierPipeline(this, modules);
        if (errMgr.getErrorCount() > 0) return; //I just really don't want to analyze something erroneous..
        analysisPipe.process();
        if (errMgr.getErrorCount() > initialErrCt) {
            return;
        }
        codegenPipe.process();
        vcsPipe.process();
    }

    @NotNull
    public List<AnnotatedModule> sortTargetModulesByUsesReferences(@NotNull AnnotatedModule... m) {
        return sortTargetModulesByUsesReferences(Arrays.asList(m));
    }

    @NotNull
    public List<AnnotatedModule> sortTargetModulesByUsesReferences(@NotNull List<AnnotatedModule> modules) {
        Map<String, AnnotatedModule> roots = new HashMap<>();
        for (AnnotatedModule module : modules) {
            roots.put(module.getModuleIdentifier().getFile().toString(), module);
        }
        return sortTargetModulesByUsesReferences(roots);
    }

    @NotNull
    public List<AnnotatedModule> sortTargetModulesByUsesReferences(@NotNull Map<String, AnnotatedModule> modules) {
        DefaultDirectedGraph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (AnnotatedModule t : Collections.unmodifiableCollection(modules.values())) {
            g.addVertex(t.getModuleIdentifier().getFile().getAbsolutePath());
            findDependencies(g, t, modules);
        }
        List<AnnotatedModule> finalOrdering = new ArrayList<>();
        List<String> intermediateOrdering = getCompileOrder(g);
        for (String s : getCompileOrder(g)) {
            AnnotatedModule m = modules.get(s);
            if (m.hasParseErrors) {
                finalOrdering.clear();
                break;
            }
            finalOrdering.add(m);
        }
        return finalOrdering;
    }

    private void findDependencies(@NotNull DefaultDirectedGraph<String, DefaultEdge> g,
                                  @NotNull AnnotatedModule root,
                                  @NotNull Map<String, AnnotatedModule> roots) {
        for (ModuleIdentifier importRequest : root.uses) {
            AnnotatedModule module = roots.get(importRequest.getFile().getAbsolutePath());
            if (module == null) {
                module = parseModule(importRequest.getFile().getAbsolutePath());
                if (module != null) {
                    roots.put(module.getModuleIdentifier().getFile().getAbsolutePath(), module);
                }
            }
            if (module != null) {
                if (pathExists(g, module.getNameToken().getText(), root.getNameToken().getText())) {
                    errMgr.semanticError(ErrorKind.CIRCULAR_DEPENDENCY,
                            importRequest.getNameToken(), root.getNameToken().getText(),
                            importRequest.getNameToken().getText());
                    break;
                }
                Graphs.addEdgeWithVertices(g, root.getModuleIdentifier().getFile().getAbsolutePath(),
                        module.getModuleIdentifier().getFile().getAbsolutePath());
                findDependencies(g, module, roots);
            }
        }
    }

    private List<String> getCompileOrder(DefaultDirectedGraph<String, DefaultEdge> g) {
        List<String> result = new ArrayList<>();
        EdgeReversedGraph<String, DefaultEdge> reversed = new EdgeReversedGraph<>(g);
        TopologicalOrderIterator<String, DefaultEdge> dependencies = new TopologicalOrderIterator<>(reversed);
        while (dependencies.hasNext()) {
            result.add(dependencies.next());
        }
        return result;
    }

    private boolean pathExists(@NotNull DefaultDirectedGraph<String, DefaultEdge> g,
                               @NotNull String src,
                               @NotNull String dest) {
        //If src doesn't exist in g, then there is obviously no path from
        //src -> ... -> dest
        if (!g.containsVertex(src)) {
            return false;
        }
        GraphIterator<String, DefaultEdge> iterator = new DepthFirstIterator<>(g, src);
        while (iterator.hasNext()) {
            String next = iterator.next();
            //we've reached dest from src -- a path exists.
            if (next.equals(dest)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public AnnotatedModule parseModule(@NotNull String fileName) {
        try {
            File file = new File(fileName);
            if (!file.isAbsolute()) {
                file = new File(libDirectory, fileName);    //first try searching in the local project..
            }
            if (!file.exists()) return null;
            return parseModule(new ANTLRFileStream(file.getCanonicalPath()));
        } catch (IOException ioe) {
            errMgr.toolError(ErrorKind.CANNOT_OPEN_FILE, ioe, fileName);
        }
        return null;
    }

    @Nullable
    public AnnotatedModule parseModule(CharStream input) {
        ResolveLexer lexer = new ResolveLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);
        ResolveParser parser = new ResolveParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errMgr);
        ParserRuleContext start = parser.moduleDecl();
        Token moduleNameTok = null;
        try {
            moduleNameTok = Utils.getModuleCtxName(start);
        } catch (IllegalArgumentException iae) {
            return null;
        }
        boolean hasParseErrors = parser.getNumberOfSyntaxErrors() > 0;

        //if we have syntactic errors, better not risk processing imports with
        //our tree (as it usually will result in a flurry of npe's).
        UsesListener l = new UsesListener(this);
        if (!hasParseErrors) {
            ParseTreeWalker.DEFAULT.walk(l, start);
        }
        return new AnnotatedModule(start, moduleNameTok, parser.getSourceName(), hasParseErrors, l.uses, l.extUses);
    }

    @NotNull
    public static String getCoreLibraryDirectory() {
        String resolveRoot = System.getenv("RESOLVEROOT");
        return resolveRoot == null ? "." : resolveRoot;
    }

    @NotNull
    public static String getLibrariesPathDirectory() {
        String resolvePath = System.getenv("RESOLVEPATH");
        return resolvePath == null ? "." : resolvePath;
    }

    /**
     * Used primarily by codegen to create new output files. If {@code outputDirectory} (set by -o) isn't present it
     * will be created. The final filename is sensitive to the output directory and the directory where the soure file
     * was found in.  If -o is /tmp and the original source file was foo/t.resolve then output files go in /tmp/foo.
     * <p>
     * Default behavior of this file writer is to just output stuff to whatever folder specified by -o in the project
     * root directory {@code libDirectory}, which is specified by the user.
     * <p>
     * If no -o is specified, then just write to the directory where the sourcefile was found; and if
     * {@code outputDirectory==null} then write a String.
     */
    public Writer getOutputFileWriter(@NotNull ModuleIdentifier module,
                                      @NotNull String outputFileName) throws IOException {
        return getOutputFileWriter(module, outputFileName, new Function<String, File>() {
            @Override
            public File apply(String filePath) {
                File outputDir;
                String fileDirectory = libDirectory;
                if (haveOutputDir) {
                    if (new File(fileDirectory).isAbsolute() || fileDirectory.startsWith("~")) {
                        outputDir = new File(outputDirectory);
                    }
                    else {
                        outputDir = new File(outputDirectory, fileDirectory);
                    }
                }
                else {
                    outputDir = new File(fileDirectory);
                }
                return outputDir;
            }
        });
    }

    public Writer getOutputFileWriter(@NotNull ModuleIdentifier module,
                                      @NotNull String outputFileName,
                                      @NotNull Function<String, File> outputDirFun) throws IOException {
        if (outputDirectory == null) {
            return new StringWriter();
        }
        File outputDir = outputDirFun.apply(module.getFile().getPath());
        File outputFile = new File(outputDir, outputFileName);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        OutputStreamWriter osw;
        osw = new OutputStreamWriter(fos);

        return new BufferedWriter(osw);
    }

    public void addListener(@Nullable RESOLVECompilerListener cl) {
        if (cl != null) listeners.add(cl);
    }

    public void removeListener(@Nullable RESOLVECompilerListener tl) {
        listeners.remove(tl);
    }

    public void removeListeners() {
        listeners.clear();
    }

    @NotNull
    public List<RESOLVECompilerListener> getListeners() {
        return listeners;
    }

    public void version() {
        info("RESOLVE Compiler Version " + VERSION);
    }

    private void help() {
        version();
        for (Option o : optionDefs) {
            String name = o.name + (o.argType != OptionArgType.NONE ? " ___" : "");
            String s = String.format(" %-19s %s", name, o.description);
            info(s);
        }
    }

    public void log(@Nullable String component, @NotNull String msg) {
        logMgr.log(component, msg);
    }

    public void log(@NotNull String msg) {
        log(null, msg);
    }

    public void info(@NotNull String msg) {
        if (listeners.isEmpty()) {
            defaultListener.info(msg);
            return;
        }
        for (RESOLVECompilerListener l : listeners) l.info(msg);
    }

    public void error(@NotNull RESOLVEMessage msg) {
        if (listeners.isEmpty()) {
            defaultListener.error(msg);
            return;
        }
        for (RESOLVECompilerListener l : listeners) l.error(msg);
    }

    public void warning(@NotNull RESOLVEMessage msg) {
        if (listeners.isEmpty()) {
            defaultListener.warning(msg);
        }
        else {
            for (RESOLVECompilerListener l : listeners) l.warning(msg);
        }
    }

    private void exit(int e) {
        System.exit(e);
    }
}
