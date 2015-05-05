package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
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

    DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
        this.g = symtab.getTypeGraph();
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

    @Override public void exitFacilityDecl(
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
        MTType modelType = null;
        try {
            //NOTE:
            //Can't walk the whole ctx here just yet. Say exemplar is 'b', and the
            //initialization stipulates that b = true. Since we only just get around
            //to adding the (typed) binding for exemplar 'b' below, we won't be
            //able to properly type all of 'ctx's subexpressions right here.
            annotateExps(ctx.mathTypeExp());
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
        //now annotate types for all subexpressions within the typeModelDecl
        //tree (e.g. contraints, init, final, etc) before leaving the scope.
        annotateExps(ctx);
        symtab.endScope();
        if ( ctx.mathTypeExp().getText().equals(ctx.name.getText()) ) {
            compiler.errorManager.semanticError(ErrorKind.INVALID_MATH_MODEL,
                    ctx.mathTypeExp().getStart(), ctx.mathTypeExp().getText());
        }
        ParserRuleContext constraint = ctx.constraintClause() != null ?
                ctx.constraintClause() : null;
        ParserRuleContext initRequires = ctx.typeModelInit() != null ?
                ctx.typeModelInit().requiresClause() : null;
        ParserRuleContext initEnsures = ctx.typeModelInit() != null ?
                ctx.typeModelInit().ensuresClause() : null;
        ParserRuleContext finalRequires = ctx.typeModelFinal() != null ?
                ctx.typeModelFinal().requiresClause() : null;
        ParserRuleContext finalEnsures = ctx.typeModelFinal() != null ?
                ctx.typeModelFinal().ensuresClause() : null;
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgTypeModelSymbol(symtab.getTypeGraph(), ctx.name
                            .getText(), modelType, new PTFamily(modelType,
                            ctx.name.getText(), ctx.exemplar.getText(),
                            buildPExp(constraint), buildPExp(initRequires),
                            buildPExp(initEnsures), buildPExp(finalRequires),
                            buildPExp(finalEnsures)), exemplar, ctx,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        symtab.startScope(ctx);
        try {
            if ( ctx.type() != null ) {
                symtab.getInnermostActiveScope().define(
                        new ProgVariableSymbol(ctx.name.getText(), ctx,
                                getProgramType(ctx.type()), getRootModuleID()));
            }
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        annotateExps(ctx); //annotate all exps before we leave
        symtab.endScope();
        insertFunction(ctx.name, ctx, ctx.type());
    }

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        try {
            if ( ctx.type() != null ) {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, ctx.name.getText(), getProgramType(
                                ctx.type()).toMath(), null, ctx,
                                getRootModuleID()));
            }
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        annotateExps(ctx); //annotate all exps before we leave
        symtab.endScope();
        insertFunction(ctx.name, ctx, ctx.type());
    }

    @Override public void exitParameterDeclGroup(
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {
        PTType programType = getProgramType(ctx.type());
        for (TerminalNode t : ctx.Identifier()) {
            try {
                ProgParameterSymbol.ParameterMode mode =
                        ProgParameterSymbol.getModeMapping().get(
                                ctx.parameterMode().getText());
                symtab.getInnermostActiveScope().define(
                        new ProgParameterSymbol(symtab.getTypeGraph(), t
                                .getText(), mode, programType, ctx,
                                getRootModuleID()));
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

    private void insertVariables(List<TerminalNode> terminalGroup,
            ResolveParser.TypeContext type) {
        PTType programType = getProgramType(type);
        for (TerminalNode t : terminalGroup) {
            try {
                ProgVariableSymbol vs =
                        new ProgVariableSymbol(t.getText(), t, programType,
                                getRootModuleID());
                symtab.getInnermostActiveScope().define(vs);
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    private void insertFunction(@NotNull Token name, ParserRuleContext ctx,
            @Nullable ResolveParser.TypeContext type) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.scopes.get(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(symtab.getTypeGraph(), name.getText(),
                            ctx, getProgramType(type), getRootModuleID(),
                            params));
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

    protected PExp buildPExp(ParserRuleContext ctx) {
        if ( ctx == null ) {
            return g.getTrueExp();
        }
        PExpBuildingListener<PExp> builder =
                new PExpBuildingListener<PExp>(tree.mathTypes,
                        tree.mathTypeValues);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }

    /**
     * Annotates all expressions (and subexpressions) in ({@link ParseTree} ctx
     * with type info.
     * 
     * @param ctx The subtree to annotate.
     */
    protected final void annotateExps(@NotNull ParseTree ctx) {
        ComputeTypes annotator = new ComputeTypes(compiler, symtab, tree);
        ParseTreeWalker.DEFAULT.walk(annotator, ctx);
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
