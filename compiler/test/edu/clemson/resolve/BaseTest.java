package edu.clemson.resolve;

import edu.clemson.resolve.compiler.DefaultCompilerListener;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.compiler.RESOLVEMessage;
import org.antlr.v4.runtime.misc.Utils;
import org.junit.Before;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public abstract class BaseTest {
    private static final Logger LOGGER = Logger.getLogger(BaseTest.class.getName());

    public static final String newline = System.getProperty("line.separator");
    public static final String pathSep = System.getProperty("path.separator");

    /**
     * The base test directory is the directory where generated files get placed
     * during unit test execution.
     * <p>
     * The value for this property is the {@code java.io.tmpdir} system
     * property.</p>
     */
    public static final String BASE_TEST_DIR = System.getProperty("java.io.tmpdir");

    public String tmpdir = null;

    @Before public void setUp() throws Exception {
        tmpdir = new File(BASE_TEST_DIR).getAbsolutePath();
    }

    @org.junit.Rule public final TestRule testWatcher = new TestWatcher() {

        @Override protected void succeeded(Description description) {
            eraseTempDir();
        }
    };

    protected void eraseFiles() {
        if (tmpdir == null) {
            return;
        }
        File tmpdirF = new File(tmpdir);
        String[] files = tmpdirF.list();
        for(int i = 0; files!=null && i < files.length; i++) {
            new File(tmpdir+"/"+files[i]).delete();
        }
    }

    protected void eraseTempDir() {
        File tmpdirF = new File(tmpdir);
        if ( tmpdirF.exists() ) {
            eraseFiles();
            tmpdirF.delete();
        }
    }

    protected RESOLVECompiler newCompiler(String[] args) {
        return new RESOLVECompiler(args);
    }

    protected RESOLVECompiler newCompiler() {
        return new RESOLVECompiler(new String[] {"-o", tmpdir});
    }

    public void testErrors(String[] pairs, String moduleName) {
        testErrors(pairs, moduleName, new String[]{});
    }

    public void testErrors(String[] pairs, String moduleName,
                           String ... compilerOptions) {
        for (int i = 0; i < pairs.length; i+=2) {
            String input = pairs[i];
            String expected = pairs[i + 1];

            String fileName = moduleName+RESOLVECompiler.FILE_EXTENSION;
            ErrorCollector errors = resolve(fileName, input, false,
                    compilerOptions);

            String actual = errors.toString(true);
            actual = actual.replace(tmpdir + File.separator, "");
            System.err.println(actual);
            String msg = input;
            msg = msg.replace("\n","\\n");
            msg = msg.replace("\r","\\r");
            msg = msg.replace("\t","\\t");

            assertEquals("error in: "+msg, expected, actual);
        }
    }

    protected ErrorCollector resolve(String moduleFileName, String moduleStr,
                             boolean defaultListener, String ... extraOptions) {
        mkdir(tmpdir);
        writeFile(tmpdir, moduleFileName, moduleStr);
        return resolve(moduleFileName, defaultListener, extraOptions);
    }

    protected ErrorCollector resolve(String moduleFileName,
                             boolean defaultListener, String ... extraOptions) {
        final List<String> options = new ArrayList<>();
        Collections.addAll(options, extraOptions);
        if ( !options.contains("-o") ) {
            options.add("-o");
            options.add(tmpdir);
        }
        if ( !options.contains("-lib") ) {
            options.add("-lib");
            options.add(tmpdir);
        }
        options.add(new File(tmpdir, moduleFileName).toString());
        final String[] optionsA = new String[options.size()];
        options.toArray(optionsA);
        RESOLVECompiler resolve = new RESOLVECompiler(optionsA);
        ErrorCollector equeue = new ErrorCollector(resolve);
        resolve.addListener(equeue);
        if (defaultListener) {
            resolve.addListener(new DefaultCompilerListener(resolve));
        }
        resolve.processCommandLineTargets();

        if ( !defaultListener && !equeue.errors.isEmpty() ) {
            System.err.println("resolve reports errors from "+options);
            for (int i = 0; i < equeue.errors.size(); i++) {
                RESOLVEMessage msg = equeue.errors.get(i);
                System.err.println(msg);
            }
            System.out.println("!!!\nmodule:");
            try {
                System.out.println(new String(Utils.readFile(tmpdir+"/"+moduleFileName)));
            }
            catch (IOException ioe) {
                System.err.println(ioe.toString());
            }
            System.out.println("###");
        }
        if ( !defaultListener && !equeue.warnings.isEmpty() ) {
            System.err.println("resolve reports warnings from "+options);
            for (int i = 0; i < equeue.warnings.size(); i++) {
                RESOLVEMessage msg = equeue.warnings.get(i);
                System.err.println(msg);
            }
        }
        return equeue;
    }

    /**
     * Loads a collection of module strings into {@code tmpdir}.
     *
     * @throws IllegalArgumentException if the lengths of {@code modules} and
     *          {@code names} differ.
     * @param modules a list of strings describing modules.
     * @param names names of the modules (in the same order they appear
     *              in the {@code modules}).
     */
    protected void writeModules(String[] modules, String... names) {
        if (modules.length != names.length) {
            throw new IllegalArgumentException(
                    "modules.length != names.length!");
        }
        mkdir(tmpdir);
        for (int i = 0; i < modules.length; i++) {
            String inputModule = modules[i];
            String fileName = names[i]+RESOLVECompiler.FILE_EXTENSION;
            //write all of our test modules to tmpdir
            writeFile(tmpdir, fileName, inputModule);
        }
    }

    public static void writeFile(String dir, String fileName, String content) {
        try {
            Utils.writeFile(dir + "/" + fileName, content, "UTF-8");
        }
        catch (IOException ioe) {
            System.err.println("can't write file");
            ioe.printStackTrace(System.err);
        }
    }

    protected void mkdir(String dir) {
        File f = new File(dir);
        f.mkdirs();
    }
}
