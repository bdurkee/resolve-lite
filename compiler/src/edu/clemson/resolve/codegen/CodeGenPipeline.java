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
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull RESOLVECompiler compiler,
                           @NotNull List<AnnotatedTree> compilationUnits) {
        super(compiler, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;
        File external = new File(RESOLVECompiler.getCoreLibraryDirectory()
                + File.separator + "external");
        for (AnnotatedTree unit : compilationUnits) {
            if (unit.getRoot().getChild(0) instanceof
                    ResolveParser.PrecisModuleContext) continue;

            JavaCodeGenerator gen =
                    new JavaCodeGenerator(compiler, unit);
            ST generatedST = gen.generateModule();
            //System.out.println("t="+generatedST.render());
        }
    }
}