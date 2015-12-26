package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.ModuleIdentifier;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull RESOLVECompiler compiler,
                           @NotNull List<AnnotatedModule> compilationUnits) {
        super(compiler, compilationUnits);
    }

    @Override public void process() {
        if ( !compiler.genCode ) return;
        File external = new File(RESOLVECompiler.getCoreLibraryDirectory()
                + File.separator + "external");
        for (AnnotatedModule unit : compilationUnits) {
            ParseTree t = unit.getRoot().getChild(0);
            if (t instanceof ResolveParser.PrecisModuleDeclContext ||
                t instanceof ResolveParser.PrecisExtensionModuleDeclContext) continue;

            JavaCodeGenerator gen = new JavaCodeGenerator(compiler, unit);
            ST generatedST = gen.generateModule();
            System.out.println("t="+generatedST.render());
            gen.writeFile(generatedST);
            writeReferencedExternalFiles(unit);
        }
    }
    private void writeReferencedExternalFiles(AnnotatedModule unit) {
        for (ModuleIdentifier e : unit.externalUses.values()) {
            File f = new File("resources/externaljava/" + e.getNameString());
            try {
                Utils.writeFile(compiler.outputDirectory, f.getName() + ".java",
                        Utils.readFile(f.getAbsolutePath()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}