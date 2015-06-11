package org.resolvelite;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.ResolveTokenFactory;
import org.resolvelite.parsing.ResolveLexer;
import org.resolvelite.parsing.ResolveParser;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

public abstract class BaseTest {
    private static final Logger LOGGER = Logger.getLogger(BaseTest.class
            .getName());

    public static final String newline = System.getProperty("line.separator");
    public static final String pathSep = System.getProperty("path.separator");

    /**
     * The base test directory is the directory where generated files get placed
     * during unit test execution.
     * <p>
     * The default value for this property is the {@code java.io.tmpdir} system
     * property.
     * </p>
     */
    public static final String BASE_TEST_DIR = System
            .getProperty("java.io.tmpdir");

    /**
     * Build up the full classpath we need, including the surefire path (if
     * present)
     */
    public static final String CLASSPATH = System
            .getProperty("java.class.path");
    public String tmpdir = null;

    @org.junit.Rule public final TestRule testWatcher = new TestWatcher() {

        @Override protected void succeeded(Description description) {
            //remove tmpdir if no error.
            eraseTempDir();
        }
    };

    @Before public void setUp() throws Exception {
        String testDirectory =
                getClass().getSimpleName() + "-" + System.currentTimeMillis();
        tmpdir = new File(BASE_TEST_DIR, testDirectory).getAbsolutePath();
    }

    protected void eraseFiles() {
        if ( tmpdir == null ) return;
        File tmpdirF = new File(tmpdir);
        String[] files = tmpdirF.list();
        for (int i = 0; files != null && i < files.length; i++) {
            new File(tmpdir + "/" + files[i]).delete();
        }
    }

    protected void eraseTempDir() {
        File tmpdirF = new File(tmpdir);
        if ( tmpdirF.exists() ) {
            eraseFiles();
            tmpdirF.delete();
        }
    }

    protected ResolveCompiler newCompiler(String[] args) {
        ResolveCompiler compiler = new ResolveCompiler(args);
        return compiler;
    }

}