package edu.clemson.resolve.misc;

import edu.clemson.resolve.codegen.CodeGenPipeline;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.antlr.v4.runtime.misc.NotNull;

import javax.tools.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Archiver {

    public static final String BASE_CLASS_DIR = System.getProperty("java.io.tmpdir");

    private final List<CodeGenPipeline.JarUnit> rawJavaSrcs = new ArrayList<>();
    private final String entryPointName, tmpdir;
    private final RESOLVECompiler resolveCompiler;

    public Archiver(RESOLVECompiler rc, String entryPoint,
                    List<CodeGenPipeline.JarUnit> javaSrcsToPackage) {
        this.resolveCompiler = rc;
        this.rawJavaSrcs.addAll(javaSrcsToPackage);
        this.entryPointName = entryPoint;

        //create the temp dir that will house our .java and .class files.
        this.tmpdir = new File(BASE_CLASS_DIR).getAbsolutePath();
    }

    public void archive() {
        runJavaCompiler();
    }

    public void runJavaCompiler() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        GenCodeDiagnosticListener listener =
                new GenCodeDiagnosticListener(resolveCompiler);
        StandardJavaFileManager fileManager  =
                compiler.getStandardFileManager(listener, null, null);

        List<String> filesToCompile = new ArrayList<>();
        //write all .java files to tmpdir
        for (CodeGenPipeline.JarUnit u : rawJavaSrcs) {
            String javaClassName = u.javaClassName+".java";
            Utils.writeFile(tmpdir, javaClassName, u.javaClassSrc);
            filesToCompile.add(tmpdir + File.separator + javaClassName);
        }

        Iterable<? extends JavaFileObject> fileObjects =
                fileManager.getJavaFileObjectsFromStrings(filesToCompile);
        JavaCompiler.CompilationTask task = compiler.getTask(null,
                fileManager, listener, null, null, fileObjects);
        Boolean result = task.call(); // Line 7
        if ( result ){
            System.out.println("Compilation has succeeded");
        }
        eraseTempDir();
    }

    //eraseTempDir();

    protected void eraseFiles() {
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

    class GenCodeDiagnosticListener implements DiagnosticListener {
        private final RESOLVECompiler compiler;

        public GenCodeDiagnosticListener(RESOLVECompiler rc) {
            this.compiler = rc;
        }
        @Override public void report(Diagnostic diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                compiler.errMgr.toolError(ErrorKind.GENERATED_JAVA_ERROR,
                        diagnostic.getMessage(Locale.ENGLISH));
            }
        }
    }
}
