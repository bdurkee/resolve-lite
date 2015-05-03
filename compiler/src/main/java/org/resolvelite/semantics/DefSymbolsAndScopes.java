package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.semantics.programtype.PTFamily;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.programtype.PTVoid;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.List;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    protected ResolveCompiler compiler;
    protected SymbolTable symtab;
    protected AnnotatedTree tree;
    protected TypeGraph g;
    private final ComputeTypes exps;

    DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
        this.exps = new ComputeTypes(rc, symtab, annotatedTree);
        this.g = symtab.getTypeGraph();
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports
                        .getImportsOfType(ImportCollection.ImportType.NAMED));
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

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        MathSymbol exemplar = null;
        MTType modelType = null;
        try {
            //Can't walk the whole ctx here. Say exemplar is 'b', and the
            //initialization stipulates that b = true. Since we just get around
            //to adding the (typed) binding for exemplar 'b' below, we won't be
            //able to properly type all of 'ctx's subexpressions right here.
            ParseTreeWalker.DEFAULT.walk(exps, ctx.mathTypeExp());
            modelType = tree.mathTypeValues.get(ctx.mathTypeExp());
            exemplar =
                    symtab.getInnermostActiveScope()
                            .define(new MathSymbol(symtab.getTypeGraph(),
                                    ctx.exemplar.getText(), modelType, null,
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
        //now annotate types for all subexpressions within the typeModelDecl
        //tree including contraints, init, final, etc.
        ParseTreeWalker.DEFAULT.walk(exps, ctx);
        PExp constraint =
                ctx.constraintClause() != null ? buildPExp(ctx
                        .constraintClause()) : null;
        PExp initRequires =
                ctx.typeModelInit() != null ? buildPExp(ctx.typeModelInit()
                        .requiresClause()) : null;
        PExp initEnsures =
                ctx.typeModelInit() != null ? buildPExp(ctx.typeModelInit()
                        .ensuresClause()) : null;
        PExp finalRequires =
                ctx.typeModelFinal() != null ? buildPExp(ctx.typeModelFinal()
                        .requiresClause()) : null;
        PExp finalEnsures =
                ctx.typeModelFinal() != null ? buildPExp(ctx.typeModelFinal()
                        .ensuresClause()) : null;
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgTypeModelSymbol(symtab.getTypeGraph(), ctx.name
                            .getText(), modelType, new PTFamily(modelType,
                            ctx.name.getText(), ctx.exemplar.getText(),
                            constraint, initRequires, initEnsures,
                            finalRequires, finalEnsures), exemplar, ctx,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        try {
            PTType programmaticReturnType =
                    getProgramType(ctx.type(), ctx.type().qualifier,
                            ctx.type().name);
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, ctx.name.getText(),
                            programmaticReturnType.toMath(), null, ctx,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.endScope();
        insertFunction(ctx.name, ctx, ctx.type());
    }

    private void insertFunction(@NotNull Token name, ParserRuleContext ctx,
            @Nullable ResolveParser.TypeContext type) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.scopes.get(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(symtab.getTypeGraph(), name.getText(),
                            ctx, getProgramType(type), getRootModuleID(), params));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

    protected PTType getProgramType(@Nullable ResolveParser.TypeContext type) {
        return type == null ? PTVoid.getInstance(g) : getProgramType(type,
                type.qualifier, type.name);
    }

    protected PTType getProgramType(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull Token typeName) {
        return getProgramType(ctx, qualifier != null ? qualifier.getText()
                : null, typeName.getText());
    }

    /**
     * For returning symbols representing a basic type such as Integer,
     * Boolean, Character, etc
     */
    protected PTType getProgramType(@NotNull ParserRuleContext ctx,
            @Nullable String qualifier, @NotNull String typeName) {
        ProgTypeSymbol result = null;
        try {
            return symtab.getInnermostActiveScope()
                    .queryForOne(new NameQuery(qualifier, typeName, true))
                    .toProgTypeSymbol().getProgramType();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(),
                    ctx.getStart(), typeName);
        }
        return PTInvalid.getInstance(g);
    }

    protected <T extends PExp> T buildPExp(ParserRuleContext ctx) {
        if ( ctx == null ) return null;
        PExpBuildingListener<T> builder =
                new PExpBuildingListener<T>(tree.mathTypes, tree.mathTypeValues);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
