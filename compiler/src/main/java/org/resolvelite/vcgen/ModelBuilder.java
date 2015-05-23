package org.resolvelite.vcgen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol.DisplayStyle;
import org.resolvelite.semantics.*;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.query.OperationQuery;
import org.resolvelite.semantics.query.SymbolTypeQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.vcgen.applicationstrategies.*;
import org.resolvelite.vcgen.model.*;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds assertive code and applies proof rules to the code within.
 */
public class ModelBuilder extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final TypeGraph g;

    private final ParseTreeProperty<VCRuleBackedStat> stats =
            new ParseTreeProperty<>();
    private final Deque<VCAssertiveBlock> assertiveBlockStack =
            new LinkedList<>();
    private PExp moduleLevelRequires, moduleLevelConstraint = null;
    private VCAssertiveBlockBuilder curAssertiveBuilder = null;
    private final VCOutputFile outputCollector = new VCOutputFile();
    private ModuleScopeBuilder moduleScope = null;

    private final static RuleApplicationStrategy //
    <ResolveParser.CallStmtContext> EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy<ResolveParser.CallStmtContext>();

    private final static RuleApplicationStrategy //
    <ResolveParser.SwapStmtContext> SWAP_APPLICATION =
            new SwapApplicationStrategy();

    private final static RuleApplicationStrategy //
    <ResolveParser.AssignStmtContext> FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();

    private final static RuleApplicationStrategy //
    <ResolveParser.VariableDeclGroupContext> VAR_DECL_APPLICATION =
            new VarDeclApplicationStrategy();

    private final static RuleApplicationStrategy //
    <ResolveParser.RecordVariableDeclGroupContext> RECORD_VAR_DECL_APPLICATION =
            new RecordVarDeclApplicationStrategy();

    public ModelBuilder(VCGenerator gen, SymbolTable symtab) {
        this.symtab = symtab;
        this.tr = gen.getModule();
        this.g = symtab.getTypeGraph();
    }

    public VCOutputFile getOutputFile() {
        return outputCollector;
    }

    @Override public void enterModule(@NotNull ResolveParser.ModuleContext ctx) {
        moduleScope = symtab.moduleScopes.get(Utils.getModuleName(ctx));
    }

    @Override public void exitModule(@NotNull ResolveParser.ModuleContext ctx) {
        moduleLevelRequires = null;
        moduleLevelConstraint = null;
        moduleScope = null;
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        moduleLevelRequires = symtab.mathPExps.get(ctx.requiresClause());
        moduleLevelConstraint = conjunctGlobalAssertions(ctx, isConstraint());
    }

    @Override public void enterEnhancementImplModule(
            @NotNull ResolveParser.EnhancementImplModuleContext ctx) {
        moduleLevelRequires = conjunctGlobalAssertions(ctx, isRequires());
        moduleLevelConstraint = conjunctGlobalAssertions(ctx, isConstraint());

    }

    private PExp conjunctGlobalAssertions(ParserRuleContext scopedCtx,
                                          Predicate<Symbol> clauseType) {
        List<GlobalMathAssertionSymbol> wrappedAssertions =
                symtab.scopes.get(scopedCtx).query(
                        new SymbolTypeQuery<GlobalMathAssertionSymbol>(
                                GlobalMathAssertionSymbol.class));
        if (wrappedAssertions.isEmpty()) {
            return g.getTrueExp();
        }
        return g.formConjuncts(wrappedAssertions.stream().filter(clauseType)
                .map(e -> symtab.mathPExps.get(e.getAssertion()))
                .collect(Collectors.toList()));
    }

    @Override public void enterFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        throw new UnsupportedOperationException("not yet supported by vcgen");
    }

    @Override public void exitFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        throw new UnsupportedOperationException("not yet supported by vcgen");
    }

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        PExp topAssume = modifyRequiresByParams(ctx, ctx.requiresClause());
        PExp bottomConfirm = modifyEnsuresByParams(ctx, ctx.ensuresClause());

        curAssertiveBuilder =
                new VCAssertiveBlockBuilder(g, s, ctx, tr)
                        .freeVars(getFreeVars(s))
                        //
                        .assume(moduleLevelRequires).assume(topAssume)
                        .remember().finalConfirm(bottomConfirm);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        curAssertiveBuilder.stats(
                Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
                .stats(Utils.collect(VCRuleBackedStat.class,
                        ctx.variableDeclGroup(), stats));

        outputCollector.chunks.add(curAssertiveBuilder.build());
        curAssertiveBuilder = null;
    }

    @Override public void enterProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        try {
            List<PTType> argTypes = s.query(new SymbolTypeQuery
                    <ProgParameterSymbol>(ProgParameterSymbol.class)).stream()
                    .map(ProgParameterSymbol::getDeclaredType)
                    .collect(Collectors.toList());

            OperationSymbol op =
                    moduleScope.queryForOne(
                            new OperationQuery(null, ctx.name, argTypes));

            PExp topAssume = modifyRequiresByParams(ctx, op.getRequires());
            PExp bottomConfirm = modifyEnsuresByParams(ctx, op.getEnsures());

            curAssertiveBuilder =
                    new VCAssertiveBlockBuilder(g, s, ctx, tr)
                            .freeVars(getFreeVars(s)) //
                            .assume(moduleLevelRequires).assume(topAssume) //
                            .assume(moduleLevelConstraint) //
                            .finalConfirm(bottomConfirm).remember();
        }
        catch (DuplicateSymbolException|NoSuchSymbolException e) {
            e.printStackTrace();
        }
    }

    @Override public void exitProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        curAssertiveBuilder.stats(
                Utils.collect(VCRuleBackedStat.class, ctx.variableDeclGroup(),
                        stats)).stats(
                Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats));

        outputCollector.chunks.add(curAssertiveBuilder.build());
        curAssertiveBuilder = null;
    }

    @Override public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        stats.put(ctx, new VCCode<ResolveParser.VariableDeclGroupContext>(ctx,
                VAR_DECL_APPLICATION, curAssertiveBuilder));
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        stats.put(ctx,
                new VCCode<ResolveParser.RecordVariableDeclGroupContext>(ctx,
                        RECORD_VAR_DECL_APPLICATION, curAssertiveBuilder));
    }

    @Override public void exitStmt(@NotNull ResolveParser.StmtContext ctx) {
        stats.put(ctx, stats.get(ctx.getChild(0)));
    }

    @Override public void exitSwapStmt(
            @NotNull ResolveParser.SwapStmtContext ctx) {
        stats.put(ctx, new VCCode<ResolveParser.SwapStmtContext>(ctx,
                SWAP_APPLICATION, curAssertiveBuilder));
    }

    @Override public void exitAssignStmt(
            @NotNull ResolveParser.AssignStmtContext ctx) {
        stats.put(ctx, new VCCode<ResolveParser.AssignStmtContext>(ctx,
                FUNCTION_ASSIGN_APPLICATION, curAssertiveBuilder));
    }

    @Override public void exitCallStmt(
            @NotNull ResolveParser.CallStmtContext ctx) {
        stats.put(ctx, new VCCode<ResolveParser.CallStmtContext>(ctx,
                EXPLICIT_CALL_APPLICATION, curAssertiveBuilder));
    }

    private PExp modifyRequiresByParams(@NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.RequiresClauseContext requires) {
        return normalizePExp(requires); //Todo.
    }

    private PExp modifyEnsuresByParams(@NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.EnsuresClauseContext ensures) {
        List<ProgParameterSymbol> params =
                symtab.scopes.get(functionCtx).query(
                        new SymbolTypeQuery<>(ProgParameterSymbol.class));
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
                    init = symtab.mathPExps.get(((PTNamed) p.getDeclaredType()) //
                            .getInitializationEnsures()) //
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

    public List<Symbol> getFreeVars(Scope s) {
        return s.query(new SymbolTypeQuery<Symbol>(Symbol.class)).stream()
                .filter(x -> x instanceof ProgParameterSymbol)
                .filter(x -> x instanceof ProgVariableSymbol)
                .collect(Collectors.toList());
    }

    public static Predicate<Symbol> isRequires() {
        return s -> s.getDefiningTree() instanceof ResolveParser.RequiresClauseContext;
    }

    public static Predicate<Symbol> isConstraint() {
        return s -> s.getDefiningTree() instanceof ResolveParser.ConstraintClauseContext;
    }

    private PExp normalizePExp(ParserRuleContext ctx) {
        PExp e = symtab.mathPExps.get(ctx);
        return e != null ? e : g.getTrueExp();
    }
}
