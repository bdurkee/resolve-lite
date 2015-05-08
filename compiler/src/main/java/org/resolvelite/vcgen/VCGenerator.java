package org.resolvelite.vcgen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol.DisplayStyle;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.symbol.ProgVariableSymbol;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock.AssertiveBlockBuilder;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.vcstat.VCRuleTargetedStat;
import org.resolvelite.vcgen.vcstat.VCSwap;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates verification conditions (VCs) for all constructs in a given
 * module.
 */
public class VCGenerator extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final ResolveCompiler compiler;
    private final TypeGraph g;

    private final ParseTreeProperty<VCRuleTargetedStat> vcStats =
            new ParseTreeProperty<>();
    private final Deque<VCAssertiveBlock> assertiveBlockStack =
            new LinkedList<>();
    private PExp moduleLevelRequires, moduleLevelConstraint = null;

    private AssertiveBlockBuilder curAssertiveBuilder = null;

    public VCGenerator(@NotNull ResolveCompiler compiler,
            @NotNull SymbolTable symtab, @NotNull AnnotatedTree tree) {
        this.compiler = compiler;
        this.symtab = symtab;
        this.tr = tree;
        this.g = symtab.getTypeGraph();
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        moduleLevelRequires = normalizePExp(ctx.requiresClause());
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        List<VCRuleTargetedStat> code =
                Utils.collect(VCRuleTargetedStat.class, ctx.stmt(), vcStats);
        List<VCRuleTargetedStat> varsCode =
                Utils.collect(VCRuleTargetedStat.class,
                        ctx.variableDeclGroup(), vcStats);
        PExp conf = modifyEnsuresByParams(ctx, ctx.ensuresClause());

        curAssertiveBuilder =
                new AssertiveBlockBuilder(g, ctx, tr)
                        .freeVars(s.getSymbolsOfType(ProgParameterSymbol.class))
                        .freeVars(s.getSymbolsOfType(ProgVariableSymbol.class))
                        .assume(moduleLevelRequires) //
                        .remember() //
                        .stats(code).stats(varsCode) //
                        .confirm(conf);

        System.out.println("Procedure decl rule applied:");
        System.out.println(curAssertiveBuilder.build());

        int i = 0;
        i = 0;
    }

    private PExp modifyRequiresByParams(String functionName) {
        throw new UnsupportedOperationException("not yet");
    }

    @Override public void exitStmt(@NotNull ResolveParser.StmtContext ctx) {
        vcStats.put(ctx, vcStats.get(ctx.getChild(0)));
    }

    @Override public void exitSwapStmt(
            @NotNull ResolveParser.SwapStmtContext ctx) {
        vcStats.put(ctx, new VCSwap(ctx, curAssertiveBuilder));
    }

    private PExp modifyEnsuresByParams(@NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.EnsuresClauseContext ensures) {
        List<ProgParameterSymbol> params =
                symtab.scopes.get(functionCtx).getSymbolsOfType(
                        ProgParameterSymbol.class);
        PExp existingEnsures = normalizePExp(ensures);
        for (ProgParameterSymbol p : params) {
            PSymbolBuilder paramTemp =
                    new PSymbolBuilder(p.getName()).mathType(p
                            .getDeclaredType().toMath());

            PExp incParamExp = paramTemp.incoming(true).build();
            PExp paramExp = paramTemp.incoming(false).build();
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
                    PExp exemplar =
                            new PSymbolBuilder(
                                    ((PTNamed) p.getDeclaredType())
                                            .getExemplarName()).build();
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
