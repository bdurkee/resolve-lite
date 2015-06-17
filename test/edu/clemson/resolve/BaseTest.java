package edu.clemson.resolve;

import edu.clemson.resolve.compiler.ResolveCompiler;
import org.antlr.v4.runtime.misc.Utils;
import org.junit.Before;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

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

    protected ResolveCompiler newCompiler(String[] args) {
        ResolveCompiler compiler = new ResolveCompiler(args);
        return compiler;
    }

    protected ResolveCompiler newCompiler() {
        ResolveCompiler compiler = new ResolveCompiler(new String[] {"-o", tmpdir});
        return compiler;
    }

    public void testErrors(String[] pairs) {
        for (int i = 0; i < pairs.length; i+=2) {
            String input = pairs[i];
            String expected = pairs[i + 1];

            String[] lines = input.split("\n");
            String fileName = getFilenameFromFirstLineOfModule(lines[0]);
        }
    }

    /**
     * Far from a foolproof means of getting a module's filename, but the logic
     * well enough for finding names for our test cases.
     */
    public String getFilenameFromFirstLineOfModule(String firstLine) {
        String fileName = "A" + ResolveCompiler.FILE_EXTENSION;
        int mIndex = -1;
        int semi = firstLine.lastIndexOf(';');
        if ( firstLine.lastIndexOf("Precis")!=-1 ) {
            mIndex = firstLine.lastIndexOf("Precis");
        }
        else if ( firstLine.lastIndexOf("Concept")!=-1 ) {
            mIndex = firstLine.lastIndexOf("Concept");
        }
        if ( mIndex>=0 && semi>=0 ) {
            int space = firstLine.indexOf(' ', mIndex);
            fileName = firstLine.substring(space+1, semi)+ResolveCompiler.FILE_EXTENSION;
        }
        if ( fileName.length()==ResolveCompiler.FILE_EXTENSION.length() ) {
            fileName = "A" + ResolveCompiler.FILE_EXTENSION;
        }
        return fileName;
    }

    protected ErrorCollector resolve(String moduleFileName, String moduleStr,
                         boolean defaultListener, String... extraOptions) {
        mkdir(tmpdir);
        writeFile(tmpdir, grammarFileName, grammarStr);
        return antlr(grammarFileName, defaultListener, extraOptions);
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
