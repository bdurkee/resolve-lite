package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Archiver;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.stringtemplate.v4.ST;

import java.util.*;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull RESOLVECompiler rc,
                @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;

        //a map from a unit tree -> list of translated java sources necessary
        //to run unit (including unit itself)
        Map<AnnotatedTree, List<JarUnit>> targetUnitsToAllRequiredJavaSrcs =
                new HashMap<>();
        List<JarUnit> translatedSoFar = new ArrayList<>();
        for (AnnotatedTree unit : compilationUnits) {
            try {
                if ( unit.getRoot().getChild(0) instanceof Resolve.PrecisModuleContext )
                    continue;
                CodeGenerator gen = new CodeGenerator(compiler, unit);
                if ( compiler.genCode.equalsIgnoreCase("java") ) {
                    ST generatedST = gen.generateModule();
                    translatedSoFar.add(new JarUnit(unit.getName(), generatedST.render()));
                    if (compiler.targetNames.contains(unit.getName())) {
                        //gets everything processed so far + itself.
                        targetUnitsToAllRequiredJavaSrcs.put(unit, translatedSoFar);
                    }
                    gen.writeFile(generatedST);
                }
                else {
                    compiler.errMgr.toolError(
                            ErrorKind.CANNOT_CREATE_TARGET_GENERATOR);
                    return;
                }
            }
            catch (IllegalStateException ise) {
                return; //if the templates were unable to be loaded, etc.
            }
        }

        if ( compiler.jar ) {
            Archiver archiver;

            for (Map.Entry<AnnotatedTree, List<JarUnit>> group :
                    targetUnitsToAllRequiredJavaSrcs.entrySet()) {
                AnnotatedTree t = group.getKey();
                if (!containsValidMain(t.getRoot())) {
                    compiler.errMgr.toolError(ErrorKind.NO_MAIN_SPECIFIED,
                            t.getName());
                }
                archiver = new Archiver(compiler, group.getKey().getName(),
                        group.getValue());
                System.out.println("CREATING JAR FOR: " + t.getName());
                archiver.archive();
            }
        }
    }

    private boolean containsValidMain(ParseTree root) {
        if (root instanceof Resolve.ModuleContext) {
            root = root.getChild(0);
        }
        if (!(root instanceof Resolve.FacilityModuleContext)) return false;
        MainFunctionListener l = new MainFunctionListener();
        ParseTreeWalker.DEFAULT.walk(l, root);
        return l.containsValidMain;
    }

    protected class MainFunctionListener extends ResolveBaseListener {
        public boolean containsValidMain = false;

        @Override public void enterOperationProcedureDecl(
                @NotNull Resolve.OperationProcedureDeclContext ctx) {
            containsValidMain = (ctx.name.getText().equals("Main") ||
                    ctx.name.getText().equals("main"));
            containsValidMain = containsValidMain &&
                    ctx.operationParameterList().parameterDeclGroup().isEmpty();
        }
    }

    public class JarUnit {
        public final String javaClassName, javaClassSrc;

        public JarUnit(String className, String classSrc) {
            this.javaClassName = className;
            this.javaClassSrc = classSrc;
        }
    }
}