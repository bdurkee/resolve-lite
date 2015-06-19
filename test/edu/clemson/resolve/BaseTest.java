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
import java.util.List;
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

    protected RESOLVECompiler newCompiler(String[] args) {
        return new RESOLVECompiler(args);
    }

    protected RESOLVECompiler newCompiler() {
        return new RESOLVECompiler(new String[] {"-o", tmpdir});
    }

    protected ErrorCollector resolve(String moduleFileName,
                                     boolean defaultListener) {
        final List<String> options = new ArrayList<>();
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

    public void testErrors(String[] pairs) {
        for (int i = 0; i < pairs.length; i+=2) {
            String input = pairs[i];
            String expected = pairs[i + 1];

            String[] lines = input.split("\n");
            String fileName = getFilenameFromFirstLineOfModule(lines[0]);
        }
    }

    /**
     * Seems far from a foolproof means of getting a module's name, but
     * the logic works well enough for testing purposes.
     */
    public String getFilenameFromFirstLineOfModule(String firstLine) {
        String fileName = "A" + RESOLVECompiler.FILE_EXTENSION;
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
            fileName = firstLine.substring(space+1, semi)+ RESOLVECompiler.FILE_EXTENSION;
        }
        if ( fileName.length()== RESOLVECompiler.FILE_EXTENSION.length() ) {
            fileName = "A" + RESOLVECompiler.FILE_EXTENSION;
        }
        return fileName;
    }

    protected ErrorCollector resolve(String moduleFileName, String moduleStr,
                         boolean defaultListener) {
        mkdir(tmpdir);
        writeFile(tmpdir, moduleFileName, moduleStr);
        return resolve(moduleFileName, defaultListener);
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
