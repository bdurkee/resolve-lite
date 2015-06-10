package org.resolvelite;

import org.resolvelite.compiler.ResolveCompiler;

import java.util.logging.Logger;

public abstract class BaseTest {
    private static final Logger LOGGER = Logger.getLogger(BaseTest.class
            .getName());

    public static final String newline = System.getProperty("line.separator");
    public static final String pathSep = System.getProperty("path.separator");

    /**
     * Build up the full classpath we need, including the surefire path (if
     * present)
     */
    public static final String CLASSPATH = System
            .getProperty("java.class.path");

    protected ResolveCompiler newCompiler(String[] args) {
        ResolveCompiler compiler = new ResolveCompiler(args);
        return compiler;
    }
}