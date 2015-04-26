package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTRepresentation;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.List;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    protected ResolveCompiler compiler;
    protected SymbolTable symtab;
    protected AnnotatedTree tree;
    protected TypeGraph g;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
        this.g = symtab.getTypeGraph();
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
                        new GenericSymbol(g, generic.getText(), generic,
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

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        MathSymbol exemplar = null;
        try {
            exemplar =
                    symtab.getInnermostActiveScope()
                            .define(new MathSymbol(symtab.getTypeGraph(),
                                    ctx.exemplar.getText(), g.INVALID, null,
                                    ctx, getRootModuleID())).toMathSymbol();
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

    @Override public void enterTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {

        ProgTypeDefinitionSymbol typeDefinition = null;
        ProgVariableSymbol reprVar = null;
        try {
            typeDefinition =
                    symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(null, ctx.name, false))
                            .toProgTypeDefinitionSymbol();
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        catch (NoSuchSymbolException nsse) {
            //it's ok if we didn't find one, we'll just function as a standalone
            //representation then (think record, etc)
        }
        String exemplarName =
                typeDefinition != null ? typeDefinition.getExemplar().getName()
                        : ctx.getText().substring(0, 1).toUpperCase();
        PTType reprType =
                new PTRepresentation(symtab.getTypeGraph(),
                        PTInvalid.getInstance(g), typeDefinition);
        try {
            reprVar =
                    new ProgVariableSymbol(exemplarName, ctx, reprType,
                            getRootModuleID());
            symtab.getInnermostActiveScope().define(reprVar);
        }
        catch (DuplicateSymbolException e) {}
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgReprTypeSymbol(symtab.getTypeGraph(), ctx.name
                            .getText(), ctx, getRootModuleID(), typeDefinition,
                            reprVar, reprType, ctx.conventionClause(), null));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
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
                                .getText(), mode, PTInvalid.getInstance(g),
                                ctx, getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        insertVariables(ctx.Identifier(), ctx.type());
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        insertVariables(ctx.Identifier(), ctx.type());
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

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        symtab.endScope();
        insertFunction(ctx.name, ctx);
    }

    private void insertVariables(List<TerminalNode> terminalGroup,
            ResolveParser.TypeContext type) {
        for (TerminalNode t : terminalGroup) {
            try {
                ProgVariableSymbol vs =
                        new ProgVariableSymbol(t.getText(), t,
                                PTInvalid.getInstance(g), getRootModuleID());
                symtab.getInnermostActiveScope().define(vs);
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    private void insertFunction(@NotNull Token name, ParserRuleContext ctx) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.scopes.get(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(symtab.getTypeGraph(), name.getText(),
                            ctx, PTInvalid.getInstance(g), getRootModuleID(),
                            params));
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
