package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.programtype.PTVoid;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.List;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    protected ResolveCompiler compiler;
    protected SymbolTable symtab;
    protected AnnotatedTree tree;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
    }

    @Override public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
        for (ResolveParser.GenericTypeContext generic : ctx.genericType()) {
            try {
                symtab.getInnermostActiveScope().define(
                        new GenericSymbol(generic.getText(), generic,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.name, ctx.name.getText());
            }
        }
    }

    @Override public void exitConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void enterFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        try {
            symtab.getInnermostActiveScope().define(
                    new FacilitySymbol(ctx, getRootModuleID(), symtab));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        MathSymbol exemplar = null;
        try {
            exemplar =
                    symtab.getInnermostActiveScope()
                            .define(new MathSymbol(symtab.getTypeGraph(),
                                    ctx.exemplar.getText(), ctx,
                                    getRootModuleID())).toMathSymbol();
        }
        catch (DuplicateSymbolException e) {
            throw new RuntimeException("duplicate exemplar!??");
        }
        symtab.endScope();
        if ( ctx.mathTypeExp().getText().equals(ctx.name.getText()) ) {
            compiler.errorManager.semanticError(ErrorKind.INVALID_MATH_MODEL,
                    ctx.mathTypeExp().getStart(), ctx.mathTypeExp().getText());
        }
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgTypeDefinitionSymbol(symtab.getTypeGraph(),
                            ctx.name.getText(), exemplar, ctx,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        try {
            symtab.getInnermostActiveScope().define(new )
        }
    }

    @Override public void exitParameterDeclGroup(
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {
        for (TerminalNode t : ctx.Identifier()) {
            try {
                ProgParameterSymbol.ParameterMode mode =
                        ProgParameterSymbol.getModeMapping().get(
                                ctx.parameterMode().getText());
                symtab.getInnermostActiveScope().define(
                        new ProgParameterSymbol(symtab.getTypeGraph(), t
                                .getText(), mode, ctx, getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.endScope();
        insertFunction(ctx.name, ctx);
    }

    private void insertFunction(@NotNull Token name, ParserRuleContext ctx) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.scopes.get(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(symtab.getTypeGraph(), name.getText(),
                            ctx, getRootModuleID(), params));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
