package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.programtype.PTVoid;
import org.resolvelite.semantics.symbol.OperationSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgTypeDefinitionSymbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.List;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    ResolveCompiler compiler;
    SymbolTable symtab;
    AnnotatedTree tree;
    String rootModuleName;
    TypeGraph g;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        rootModuleName = ctx.name.getText();
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgTypeDefinitionSymbol(g, ctx.name.getText(), ctx,
                            rootModuleName));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        //
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.endScope();
        insertFunction(ctx.name, ctx);
    }

    @Override public void exitConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.endScope();
    }

    private void insertFunction(@NotNull Token name, ParserRuleContext ctx) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.scopes.get(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(name.getText(), ctx, rootModuleName,
                            params));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

}
