package org.resolvelite.vcgeneration;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.DisplayStyle;
import org.resolvelite.semantics.ModuleScopeBuilder;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.programtype.PTFamily;
import org.resolvelite.semantics.programtype.PTGeneric;
import org.resolvelite.semantics.programtype.PTRepresentation;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.OperationQuery;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                        new PSymbolBuilder("=").arguments(oldParamExp, paramExp)
                                .style(DisplayStyle.INFIX).mathType(g.BOOLEAN)
                                .desc("ensures of " + functionName
                                        + "(from "
                                        + p.getMode().toString().toLowerCase()
                                        + "parameter mode on: " + p.getName()
                                        + ")", functionCtx)
                                .build();

                existingEnsures =
                        !existingEnsures.isLiteral() ? g.formConjunct(
                                existingEnsures, equalsExp) : equalsExp;
            }
            else if ( p.getMode() == ParameterMode.CLEARS ) {
                PExp init = null;
                if ( p.getDeclaredType() instanceof PTFamily ) {
                    PExp exemplar = new PSymbolBuilder(
                            ((PTFamily) p.getDeclaredType()).getExemplarName())
                            .build();
                    init = ((PTFamily) p.getDeclaredType()) //
                                .getInitializationEnsures() //
                                .copy() //
                                .substitute(exemplar, paramExp);
                }
                else if ( p.getDeclaredType() instanceof PTRepresentation ) {
                    PExp exemplar = new PSymbolBuilder(
                            ((PTRepresentation) p.getDeclaredType())
                                    .getExemplarName())
                                    .build();
                    init = ((PTRepresentation) p.getDeclaredType()) //
                            .getInitializationEnsures() //
                            .copy() //
                            .substitute(exemplar, paramExp);
                }
                else {  //we're dealing with a generic
                    throw new UnsupportedOperationException(
                            "generics not yet handled");
                }
                existingEnsures =
                        !existingEnsures.isLiteral() ? g.formConjunct(
                                existingEnsures, init) : init;
            }
        }
        return null;
    }

    private List<PExp> getReferencedSpecificationConstraints(AnnotatedTree t) {
        //List<String>
        return null;
    }

    private PExp getPExpFor(ParserRuleContext ctx) {
        PExp e = tr.mathPExps.get(ctx);
        return e == null ? g.getTrueExp() : e;
    }

}
