package org.resolvelite.vcgen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol.DisplayStyle;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.symbol.ProgVariableSymbol;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.SwapApplicationStrategy;
import org.resolvelite.vcgen.vcstat.AssertiveCode;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.vcstat.VCCode;
import org.resolvelite.vcgen.vcstat.VCRuleBackedStat;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Generates verification conditions (VCs) for all constructs in a given
 * module.
 */
public class VCGenerator extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final ResolveCompiler compiler;
    private final TypeGraph g;

    private final ParseTreeProperty<VCRuleBackedStat> stats =
            new ParseTreeProperty<>();
    private final Deque<VCAssertiveBlock> assertiveBlockStack =
            new LinkedList<>();
    private PExp moduleLevelRequires, moduleLevelConstraint = null;
    private VCAssertiveBlockBuilder curAssertiveBuilder = null;
    private final List<AssertiveCode> assertiveCodeChunks = new ArrayList<>();

    private final static RuleApplicationStrategy<ResolveParser.SwapStmtContext> SWAP_APPLICATION =
            new SwapApplicationStrategy();

    public VCGenerator(@NotNull ResolveCompiler compiler,
            @NotNull SymbolTable symtab, @NotNull AnnotatedTree tree) {
        this.compiler = compiler;
        this.symtab = symtab;
        this.tr = tree;
        this.g = symtab.getTypeGraph();
    }

    @Override public void exitModule(@NotNull ResolveParser.ModuleContext ctx) {
        for (AssertiveCode a : assertiveCodeChunks.get(0).getApplicationSteps()) {
            System.out.println("HERE: " + a);
            System.out.println("------------------------------");

        }
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        moduleLevelRequires = normalizePExp(ctx.requiresClause());
    }

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        PExp topAssume = modifyRequiresByParams(ctx, ctx.requiresClause());
        PExp bottomConfirm = modifyEnsuresByParams(ctx, ctx.ensuresClause());

        curAssertiveBuilder =
                new VCAssertiveBlockBuilder(g, ctx, tr)
                        .freeVars(s.getSymbolsOfType(ProgParameterSymbol.class))
                        .freeVars(s.getSymbolsOfType(ProgVariableSymbol.class))
                        .assume(moduleLevelRequires).remember() //
                        .assume(topAssume) //
                        .finalConfirm(bottomConfirm);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        curAssertiveBuilder.stats(
                Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
                .stats(Utils.collect(VCRuleBackedStat.class,
                        ctx.variableDeclGroup(), stats));

        assertiveCodeChunks.add(curAssertiveBuilder.build());
        curAssertiveBuilder = null;
        int i = 0;
        i = 0;
    }

    @Override public void exitStmt(@NotNull ResolveParser.StmtContext ctx) {
        stats.put(ctx, stats.get(ctx.getChild(0)));
    }

    @Override public void exitSwapStmt(
            @NotNull ResolveParser.SwapStmtContext ctx) {
        stats.put(ctx, new VCCode<ResolveParser.SwapStmtContext>(ctx,
                SWAP_APPLICATION, curAssertiveBuilder));
    }

    private PExp modifyRequiresByParams(@NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.RequiresClauseContext requires) {
        return normalizePExp(requires);
    }

    private PExp modifyEnsuresByParams(@NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.EnsuresClauseContext ensures) {
        List<ProgParameterSymbol> params =
                symtab.scopes.get(functionCtx).getSymbolsOfType(
                        ProgParameterSymbol.class);
        PExp existingEnsures = normalizePExp(ensures);
        for (ProgParameterSymbol p : params) {
            PSymbolBuilder temp =
                    new PSymbolBuilder(p.getName()).mathType(p
                            .getDeclaredType().toMath());

            PExp incParamExp = temp.incoming(true).build();
            PExp paramExp = temp.incoming(false).build();

            if ( p.getMode() == ParameterMode.PRESERVES
                    || p.getMode() == ParameterMode.RESTORES ) {
                PExp equalsExp =
                        new PSymbolBuilder("=")
                                .arguments(paramExp, incParamExp)
                                .style(DisplayStyle.INFIX).mathType(g.BOOLEAN)
                                .build();

                existingEnsures =
                        !existingEnsures.isLiteral() ? g.formConjunct(
                                existingEnsures, equalsExp) : equalsExp;
            }
            else if ( p.getMode() == ParameterMode.CLEARS ) {
                PExp init = null;
                if ( p.getDeclaredType() instanceof PTNamed ) {
                    PTNamed t = (PTNamed) p.getDeclaredType();
                    PExp exemplar =
                            new PSymbolBuilder(t.getExemplarName()).mathType(
                                    t.toMath()).build();
                    init = ((PTNamed) p.getDeclaredType()) //
                            .getInitializationEnsures() //
                            .copy() //
                            .substitute(exemplar, paramExp);
                }
                else { //we're dealing with a generic
                    throw new UnsupportedOperationException(
                            "generics not yet handled");
                }
                existingEnsures =
                        !existingEnsures.isLiteral() ? g.formConjunct(
                                existingEnsures, init) : init;
            }
        }
        return existingEnsures;
    }

    //private PExp getModuleLevelConstraint(

    private PExp normalizePExp(ParserRuleContext ctx) {
        PExp e = tr.mathPExps.get(ctx);
        return e != null ? e.copy() : g.getTrueExp();
    }

}
