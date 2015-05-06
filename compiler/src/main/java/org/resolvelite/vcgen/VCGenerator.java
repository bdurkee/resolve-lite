package org.resolvelite.vcgen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol.DisplayStyle;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.List;

public class VCGenerator extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final ResolveCompiler compiler;
    private final TypeGraph g;

    public VCGenerator(@NotNull ResolveCompiler compiler,
            @NotNull SymbolTable symtab, @NotNull AnnotatedTree tree)
            throws NoSuchSymbolException {
        this.compiler = compiler;
        this.symtab = symtab;
        this.tr = tree;
        this.g = symtab.getTypeGraph();
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {}

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {

    }

    private PExp modifyEnsuresByParams(String functionName,
            @NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.EnsuresClauseContext ensures) {
        List<ProgParameterSymbol> params =
                symtab.scopes.get(functionCtx).getSymbolsOfType(
                        ProgParameterSymbol.class);
        PExp existingEnsures = getPExpFor(ensures);
        for (ProgParameterSymbol p : params) {
            PSymbolBuilder paramTemp =
                    new PSymbolBuilder(p.getName()).mathType(p
                            .getDeclaredType().toMath());

            PExp paramExp = paramTemp.incoming(true).build();
            PExp oldParamExp = paramTemp.build();
            if ( p.getMode() == ParameterMode.PRESERVES
                    || p.getMode() == ParameterMode.RESTORES ) {
                PExp equalsExp =
                        new PSymbolBuilder("=")
                                .arguments(oldParamExp, paramExp)
                                .style(DisplayStyle.INFIX)
                                .mathType(g.BOOLEAN)
                                .desc("ensures of " + functionName + "(from "
                                        + p.getMode().toString().toLowerCase()
                                        + "parameter mode on: " + p.getName()
                                        + ")", functionCtx).build();

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

    private PExp getPExpFor(ParserRuleContext ctx) {
        PExp e = tr.mathPExps.get(ctx);
        return e != null ? e : g.getTrueExp();
    }

}
