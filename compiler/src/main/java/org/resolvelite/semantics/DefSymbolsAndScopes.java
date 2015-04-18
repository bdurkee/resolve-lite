package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    Scope currentScope; // define symbols in this scope
    ResolveCompiler compiler;
    SymbolTable symtab;
    AnnotatedTree tree;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
           @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
    }

    @Override public void enterConceptModule(@NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }



    @Override public void exitConceptModule(@NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.endScope();
    }

}
