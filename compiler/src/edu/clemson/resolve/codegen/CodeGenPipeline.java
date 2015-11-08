package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Archiver;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    //a map from a unit tree -> list of translated java sources necessary
    //to run unit (including unit itself
    Map<AnnotatedTree, List<JavaUnit>> targetUnitsToAllRequiredJavaSrcs =
            new HashMap<>();

    public CodeGenPipeline(RESOLVECompiler rc,
                           List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;

        List<JavaUnit> translatedSoFar = new ArrayList<>();
        File external = new File(RESOLVECompiler.getCoreLibraryDirectory()
                + File.separator + "external");
        for (AnnotatedTree unit : compilationUnits) {
            try {
                if ( unit.getRoot().getChild(0) instanceof
                        ResolveParser.PrecisModuleContext ) continue;

                CodeGenerator gen = new CodeGenerator(compiler, unit);
                if ( compiler.genCode.equalsIgnoreCase("java") ) {
                    ST generatedST = gen.generateModule();
                    //System.out.println("t="+generatedST.render());
                    translatedSoFar.add(new JavaUnit(unit.getName(), generatedST.render()));

                    //Todo: Try to coalecse this and "addAdditionalFiles" into the same method
                    for (File externalFile : external.listFiles()) {
                        try {
                            String content = Utils.readFile(externalFile.getAbsolutePath());
                            String externalName =
                                    Utils.stripFileExtension(externalFile.getName());
                            if (unit.externalUses.containsKey(externalName)) {
                                translatedSoFar.add(new JavaUnit(externalName, content));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (compiler.targetNames.contains(unit.getName())) {
                        //gets everything processed so far + itself.
                        targetUnitsToAllRequiredJavaSrcs.put(unit, translatedSoFar);

                        //add external runtime java files
                        FileLocator f = new FileLocator("java");
                        Files.walkFileTree(new File(RESOLVECompiler.getCoreLibraryDirectory()
                                        + File.separator + "runtime").toPath(), f);
                        addAdditionalFiles(f.getFiles(), unit);
                    }
                    //gen.writeFile(generatedST);
                }
                else {
                    compiler.errMgr.toolError(
                            ErrorKind.CANNOT_CREATE_TARGET_GENERATOR);
                    return;
                }
            }
            catch (IllegalStateException ise) {
                return; //if the templates were unable to be loaded, etc.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Set<JavaUnit> someSet = new HashSet<>();
        targetUnitsToAllRequiredJavaSrcs.values().forEach(someSet::addAll);
        for (JavaUnit u : someSet) {
            File outputFile =
                    new File(compiler.outputDirectory + File.separator
                            + u.javaClassName + ".java");
            try {
                Files.write(outputFile.toPath(),
                        u.javaClassSrc.getBytes(Charset.forName("UTF-8")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if ( compiler.jar ) {

            for (Map.Entry<AnnotatedTree, List<JavaUnit>> group :
                    targetUnitsToAllRequiredJavaSrcs.entrySet()) {
                AnnotatedTree t = group.getKey();
                if (!containsValidMain(t.getRoot())) {
                    compiler.errMgr.toolError(ErrorKind.NO_MAIN_SPECIFIED,
                            t.getName());
                }
                Archiver archiver = new Archiver(compiler, group.getKey(),
                        group.getValue());
                archiver.archive();
            }
        }
    }

    private boolean containsValidMain(ParseTree root) {
        if (root instanceof ResolveParser.ModuleContext) {
            root = root.getChild(0);
        }
        //if (!(root instanceof ResolveParser.FacilityModuleContext)) return false;
        MainFunctionListener l = new MainFunctionListener();
        ParseTreeWalker.DEFAULT.walk(l, root);
        return l.containsMain;
    }

    protected class MainFunctionListener extends ResolveBaseListener {
        public boolean containsMain = false;

        /*@Override public void enterOperationProcedureDecl(
                ResolveParser.OperationProcedureDeclContext ctx) {
            if (ctx.name.getText().equals("Main") ||
                    ctx.name.getText().equals("main")) containsMain = true;
        }*/
    }

    private void addAdditionalFiles(List<File> files,
                                    AnnotatedTree currentUnit) {
        for (File javaFile : files) {
            try {
                String content = Utils.readFile(javaFile.getAbsolutePath());
                String name = Utils.stripFileExtension(javaFile.getName());
                targetUnitsToAllRequiredJavaSrcs.get(currentUnit)
                        .add(new JavaUnit(name, content));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class JavaUnit {
        public final String javaClassName, javaClassSrc;

        public JavaUnit(String className, String classSrc) {
            this.javaClassName = className;
            this.javaClassSrc = classSrc;
        }
        @Override public boolean equals(Object o) {
            boolean result = o instanceof JavaUnit;
            if ( result ) {
                result = ((JavaUnit)o).javaClassName.equals(this.javaClassName);
            }
            return result;
        }

        @Override public int hashCode() {
            return javaClassName.hashCode();
        }
    }
}