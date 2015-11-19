package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.Resolve;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.util.*;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull RESOLVECompiler compiler,
                           @NotNull List<AnnotatedModule> compilationUnits) {
        super(compiler, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;
        File external = new File(RESOLVECompiler.getCoreLibraryDirectory()
                + File.separator + "external");
        for (AnnotatedModule unit : compilationUnits) {
            if (unit.getRoot().getChild(0) instanceof
                    Resolve.PrecisModuleContext) continue;

            JavaCodeGenerator gen =
                    new JavaCodeGenerator(compiler, unit);
            ST generatedST = gen.generateModule();
            //System.out.println("t="+generatedST.render());
        }
    }
}