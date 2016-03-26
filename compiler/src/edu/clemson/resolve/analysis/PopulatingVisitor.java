package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.DumbTypeGraph;
import org.rsrg.semantics.MathSymbolTable;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    private final RESOLVECompiler compiler;
    private final MathSymbolTable symtab;
    private final AnnotatedModule tr;
    private final DumbTypeGraph g;

    public PopulatingVisitor(@NotNull RESOLVECompiler rc,
                             @NotNull MathSymbolTable symtab,
                             @NotNull AnnotatedModule annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    public DumbTypeGraph getTypeGraph() {
        return g;
    }

}
