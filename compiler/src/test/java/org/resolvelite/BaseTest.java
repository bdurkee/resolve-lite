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
     * Build up the full classpath we need, including the surefire path (if
     * present)
     */
    public static final String CLASSPATH = System
            .getProperty("java.class.path");
    public String tmpdir = null;

    @org.junit.Rule public final TestRule testWatcher = new TestWatcher() {

        @Override protected void succeeded(Description description) {
            // remove tmpdir if no error.
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

    public ParseTree execParser(String startRuleName, String input,
            String parserName, String lexerName) throws Exception {
        Pair<Parser, Lexer> pl =
                getParserAndLexer(input, parserName, lexerName);
        Parser parser = pl.a;
        return execStartRule(startRuleName, parser);
    }

    public ParseTree execStartRule(String startRuleName, Parser parser)
            throws IllegalAccessException,
                InvocationTargetException,
                NoSuchMethodException {
        Method startRule = null;
        Object[] args = null;
        try {
            startRule = parser.getClass().getMethod(startRuleName);
        }
        catch (NoSuchMethodException nsme) {
            // try with int _p arg for recursive func
            startRule = parser.getClass().getMethod(startRuleName, int.class);
            args = new Integer[] { 0 };
        }
        ParseTree result = (ParseTree) startRule.invoke(parser, args);
        //		System.out.println("parse tree = "+result.toStringTree(parser));
        return result;
    }

    public Pair<Parser, Lexer> getParserAndLexer(String input,
            String parserName, String lexerName) throws Exception {
        final Class<? extends Lexer> lexerClass =
                loadLexerClassFromTempDir(lexerName);
        final Class<? extends Parser> parserClass =
                loadParserClassFromTempDir(parserName);

        ANTLRInputStream in = new ANTLRInputStream(new StringReader(input));

        Class<? extends Lexer> c = lexerClass.asSubclass(Lexer.class);
        Constructor<? extends Lexer> ctor = c.getConstructor(CharStream.class);
        Lexer lexer = ctor.newInstance(in);

        Class<? extends Parser> pc = parserClass.asSubclass(Parser.class);
        Constructor<? extends Parser> pctor =
                pc.getConstructor(TokenStream.class);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = pctor.newInstance(tokens);
        return new Pair<Parser, Lexer>(parser, lexer);
    }

    public Class<?> loadClassFromTempDir(String name) throws Exception {
        ClassLoader loader =
                new URLClassLoader(
                        new URL[] { new File(tmpdir).toURI().toURL() },
                        ClassLoader.getSystemClassLoader());
        return loader.loadClass(name);
    }

    public Class<? extends Lexer> loadLexerClassFromTempDir(String name)
            throws Exception {
        return loadClassFromTempDir(name).asSubclass(Lexer.class);
    }

    public Class<? extends Parser> loadParserClassFromTempDir(String name)
            throws Exception {
        return loadClassFromTempDir(name).asSubclass(Parser.class);
    }
}