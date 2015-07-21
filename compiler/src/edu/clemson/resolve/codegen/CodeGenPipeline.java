package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull RESOLVECompiler rc,
                @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;

        //a map from a unit tree -> list of translated java sources necessary
        //to run unit (including unit itself)
        Map<AnnotatedTree, List<String>> targetUnitsToAllRequiredJavaSrcs =
                new HashMap<>();
        List<String> translatedSoFar = new ArrayList<>();
        for (AnnotatedTree unit : compilationUnits) {
            try {
                if ( unit.getRoot().getChild(0) instanceof Resolve.PrecisModuleContext )
                    continue;
                CodeGenerator gen = new CodeGenerator(compiler, unit);
                if ( compiler.genCode.equalsIgnoreCase("java") ) {
                    ST generatedST = gen.generateModule();
                    translatedSoFar.add(generatedST.render());
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
            for (Map.Entry<AnnotatedTree, List<String>> group :
                    targetUnitsToAllRequiredJavaSrcs.entrySet()) {
                AnnotatedTree t = group.getKey();
                if (!containsValidMain(t.getRoot())) {
                    compiler.errMgr.toolError(ErrorKind.NO_MAIN_SPECIFIED,
                            t.getName());
                }
                System.out.println("CREATING JAR FOR: " + t.getName());

            }
        }
    }

    private boolean containsValidMain(ParseTree root) {
        if (root instanceof Resolve.ModuleContext) {
            root = root.getChild(0);
        }
        if (!(root instanceof Resolve.FacilityModuleContext)) return false;
        MainListener l = new MainListener();
        ParseTreeWalker.DEFAULT.walk(l, root);
        return l.containsValidMain;
    }

    protected class MainListener extends ResolveBaseListener {
        public boolean containsValidMain = false;

        @Override public void enterOperationProcedureDecl(
                @NotNull Resolve.OperationProcedureDeclContext ctx) {
            containsValidMain = (ctx.name.getText().equals("Main") ||
                    ctx.name.getText().equals("main"));
            containsValidMain = containsValidMain &&
                    ctx.operationParameterList().parameterDeclGroup().isEmpty();
        }
    }
}