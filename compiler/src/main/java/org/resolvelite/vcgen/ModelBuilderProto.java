package org.resolvelite.vcgen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PSymbol.DisplayStyle;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.*;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.query.OperationQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.resolvelite.vcgen.application.ExplicitCallApplicationStrategy;
import org.resolvelite.vcgen.application.FunctionAssignApplicationStrategy;
import org.resolvelite.vcgen.application.StatRuleApplicationStrategy;
import org.resolvelite.vcgen.application.SwapApplicationStrategy;
import org.resolvelite.vcgen.model.*;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds assertive code and applies proof rules to the code within.
 */
public class ModelBuilderProto extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final TypeGraph g;

    private final ParseTreeProperty<VCRuleBackedStat> stats =
            new ParseTreeProperty<>();
    private final Deque<VCAssertiveBlock> assertiveBlockStack =
            new LinkedList<>();
    private VCAssertiveBlockBuilder curAssertiveBuilder = null;
    private final VCOutputFile outputFile = new VCOutputFile();
    private ModuleScopeBuilder moduleScope = null;
    private ResolveParser.TypeRepresentationDeclContext curTypeRepr = null;

    public static final StatRuleApplicationStrategy EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    private final static StatRuleApplicationStrategy FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();
    private final static StatRuleApplicationStrategy SWAP_APPLICATION =
            new SwapApplicationStrategy();

    public ModelBuilderProto(VCGenerator gen, SymbolTable symtab) {
        this.symtab = symtab;
        this.tr = gen.getModule();
        this.g = symtab.getTypeGraph();
    }

    public VCOutputFile getOutputFile() {
        return outputFile;
    }

    @Override public void enterModule(@NotNull ResolveParser.ModuleContext ctx) {
        moduleScope = symtab.moduleScopes.get(Utils.getModuleName(ctx));
    }

    @Override public void enterTypeImplInit(
            @NotNull ResolveParser.TypeImplInitContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        ProgReprTypeSymbol repr =
                symtab.ctxToSyms.get(ctx).toProgReprTypeSymbol();

        PExp convention = repr.getConvention();
        PExp correspondence = repr.getCorrespondence();
        PExp typeInitEnsures = g.getTrueExp();
        curAssertiveBuilder =
                new VCAssertiveBlockBuilder(g, symtab.scopes.get(ctx),
                        "T_Init_Hypo=" + repr.getName(), ctx, tr)
                        .assume(getModuleLevelAssertionsOfType(requires()));
    }

    @Override public void exitTypeImplInit(
            @NotNull ResolveParser.TypeImplInitContext ctx) {
        ProgReprTypeSymbol s = symtab.ctxToSyms.get(ctx).toProgReprTypeSymbol();
        PExp typeInitEnsures = g.getTrueExp();
        PExp convention = s.getConvention();
        PExp correspondence = s.getCorrespondence();
        if ( s.getDefinition() != null ) {
            typeInitEnsures =
                    s.getDefinition().getProgramType()
                            .getInitializationEnsures();
        }
        PExp newInitEnsures =
                typeInitEnsures.substitute(s.exemplarAsPSymbol(),
                        s.conceptualExemplarAsPSymbol());
        newInitEnsures =
                withCorrespondencePartsSubstituted(newInitEnsures,
                        correspondence);
        curAssertiveBuilder.stats(Utils.collect(VCRuleBackedStat.class,
                ctx.stmt(), stats));
        curAssertiveBuilder.confirm(convention).finalConfirm(newInitEnsures);
        outputFile.chunks.add(curAssertiveBuilder.build());
        curAssertiveBuilder = null;
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        ProgReprTypeSymbol s = symtab.ctxToSyms.get(ctx).toProgReprTypeSymbol();
        curAssertiveBuilder =
                new VCAssertiveBlockBuilder(g, symtab.scopes.get(ctx),
                        "Well_Def_Corr_Hyp=" + ctx.name.getText(), ctx, tr)
                        .assume(getModuleLevelAssertionsOfType(requires())).assume(
                                s.getConvention());

        PExp constraint = g.getTrueExp();
        PExp correspondence = s.getCorrespondence();
        if ( s.getDefinition() != null ) {
            constraint = s.getDefinition().getProgramType().getConstraint();
        }
        PExp newConstraint =
                constraint.substitute(s.exemplarAsPSymbol(),
                        s.conceptualExemplarAsPSymbol());
        newConstraint =
                withCorrespondencePartsSubstituted(newConstraint,
                        correspondence);
        curAssertiveBuilder.finalConfirm(newConstraint);
        outputFile.chunks.add(curAssertiveBuilder.build());
        curAssertiveBuilder = null;
    }

    @Override public void enterProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        try {
            List<PTType> argTypes =
                    s.getSymbolsOfType(ProgParameterSymbol.class).stream()
                            .map(ProgParameterSymbol::getDeclaredType)
                            .collect(Collectors.toList());
            OperationSymbol op = s.queryForOne(
                    new OperationQuery(null, ctx.name, argTypes));

            PExp localRequires = modifyRequiresByParams(ctx, op.getRequires());
            PExp localConfirm = modifyEnsuresByParams(ctx, op.getEnsures());

            curAssertiveBuilder =
                    new VCAssertiveBlockBuilder(g, s,
                            "Proc_Decl_Rule="+ctx.name.getText() , ctx, tr)
                            .freeVars(getFreeVars(s)) //
                            .assume(localRequires) //
                            .assume(getModuleLevelAssertionsOfType(requires()))
                            .assume(getModuleLevelAssertionsOfType(constraint()))
                            .finalConfirm(localConfirm).remember();
        }
        catch (DuplicateSymbolException|NoSuchSymbolException e) {
            e.printStackTrace();
        }
    }

    @Override public void exitProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        curAssertiveBuilder.stats(Utils.collect(VCRuleBackedStat.class,
                ctx.stmt(), stats));
        outputFile.chunks.add(curAssertiveBuilder.build());
        curAssertiveBuilder = null;
    }

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        PExp localRequires = modifyRequiresByParams(ctx, ctx.requiresClause());
        PExp localConfirm = modifyEnsuresByParams(ctx, ctx.ensuresClause());
        List<ResolveParser.ParameterDeclGroupContext> paramGroupings =
                ctx.operationParameterList().parameterDeclGroup();
        curAssertiveBuilder =
                new VCAssertiveBlockBuilder(g, s, "Proc_Decl_Rule="
                        + ctx.name.getText(), ctx, tr).freeVars(getFreeVars(s))
                        .assume(getModuleLevelAssertionsOfType(requires()))
                        .assume(getModuleLevelAssertionsOfType(constraint()))
                        .assume(localRequires)
                        .assume(getFormalParamConstraints(paramGroupings))
                        .remember().finalConfirm(localConfirm);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        curAssertiveBuilder.stats(
                Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
                .stats(Utils.collect(VCRuleBackedStat.class,
                        ctx.variableDeclGroup(), stats));

        outputFile.chunks.add(curAssertiveBuilder.build());
        curAssertiveBuilder = null;
    }

    @Override public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        modifyAssertiveBlockByVariableDecls(ctx.Identifier(), ctx.type());
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        modifyAssertiveBlockByVariableDecls(ctx.Identifier(), ctx.type());
    }

    //-----------------------------------------------
    // S T A T S
    //-----------------------------------------------

    @Override public void exitStmt(@NotNull ResolveParser.StmtContext ctx) {
        stats.put(ctx, stats.get(ctx.getChild(0)));
    }

    @Override public void exitCallStmt(
            @NotNull ResolveParser.CallStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, curAssertiveBuilder,
                        EXPLICIT_CALL_APPLICATION, tr.mathPExps.get(ctx
                                .progParamExp()));
        stats.put(ctx, s);
    }

    @Override public void exitSwapStmt(
            @NotNull ResolveParser.SwapStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, curAssertiveBuilder,
                        SWAP_APPLICATION, tr.mathPExps.get(ctx.left),
                        tr.mathPExps.get(ctx.right));
        stats.put(ctx, s);
    }

    @Override public void exitAssignStmt(
            @NotNull ResolveParser.AssignStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, curAssertiveBuilder,
                        FUNCTION_ASSIGN_APPLICATION,
                        tr.mathPExps.get(ctx.left), tr.mathPExps.get(ctx.right));
        stats.put(ctx, s);
    }

    private PExp modifyRequiresByParams(@NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.RequiresClauseContext requires) {
        return tr.getPExpFor(g, requires);
    }

    private PExp modifyEnsuresByParams(@NotNull ParserRuleContext functionCtx,
            @Nullable ResolveParser.EnsuresClauseContext ensures) {
        List<ProgParameterSymbol> params =
                symtab.scopes.get(functionCtx).getSymbolsOfType(
                        ProgParameterSymbol.class);
        PExp existingEnsures = tr.getPExpFor(g, ensures);
        for (ProgParameterSymbol p : params) {
            PSymbol.PSymbolBuilder temp =
                    new PSymbol.PSymbolBuilder(p.getName()).mathType(p
                            .getDeclaredType().toMath());

            PExp incParamExp = temp.incoming(true).build();
            PExp paramExp = temp.incoming(false).build();

            if ( p.getMode() == ParameterMode.PRESERVES
                    || p.getMode() == ParameterMode.RESTORES ) {
                PExp equalsExp =
                        new PSymbol.PSymbolBuilder("=")
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
                            new PSymbol.PSymbolBuilder(t.getExemplarName())
                                    .mathType(t.toMath()).build();
                    init = ((PTNamed) p.getDeclaredType()) //
                            .getInitializationEnsures() //
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

    /**
     * Modifies the current, working assertive block by adding initializion
     * information for declared variables to our set of assumptions.
     * 
     * @param vars A list of {@link TerminalNode}s representing the names of
     *        the declared variables.
     * @param type The syntax node containing the 'programmatic type' of all
     *        variables contained in {@code vars}.
     * @throws java.lang.IllegalStateException if the working assertive block
     *         is {@code null}.
     */
    public void modifyAssertiveBlockByVariableDecls(
            @NotNull List<TerminalNode> vars,
            @NotNull ResolveParser.TypeContext type) {
        if ( curAssertiveBuilder == null ) {
            throw new IllegalStateException("no open assertive builder");
        }
        PTType groupType = tr.progTypeValues.get(type);
        PExp finalConfirm = curAssertiveBuilder.finalConfirm.getConfirmExp();
        for (TerminalNode t : vars) {
            if ( groupType instanceof PTGeneric ) {
                curAssertiveBuilder.assume(g.formInitializationPredicate(
                        groupType, t.getText()));
            }
            else {
                PTNamed namedComponent = (PTNamed) groupType;
                PExp exemplar =
                        new PSymbol.PSymbolBuilder(
                                namedComponent.getExemplarName()).mathType(
                                groupType.toMath()).build();
                PExp variable =
                        new PSymbol.PSymbolBuilder(t.getText()).mathType(
                                groupType.toMath()).build();
                PExp init =
                        namedComponent.getInitializationEnsures().substitute(
                                exemplar, variable);
                if ( finalConfirm.containsName(t.getText()) ) {
                    curAssertiveBuilder.assume(init);
                }
            }
        }
    }

    public List<Symbol> getFreeVars(Scope s) {
        return s.getSymbolsOfType(Symbol.class).stream()
                .filter(x -> x instanceof ProgParameterSymbol ||
                        x instanceof ProgVariableSymbol)
                .collect(Collectors.toList());
    }

    public static Predicate<Symbol> constraint() {
        return s -> s.getDefiningTree() instanceof  //
                ResolveParser.ConstraintClauseContext;
    }

    public static Predicate<Symbol> requires() {
        return s -> s.getDefiningTree() instanceof //
                ResolveParser.RequiresClauseContext;
    }

    public PExp withCorrespondencePartsSubstituted(PExp start,
            PExp correspondence) {
        for (PExp e : correspondence.splitIntoConjuncts()) {
            if ( !e.isEquality() ) {
                //Todo: This should be added to ErrorKind and checked somewhere better.
                throw new IllegalStateException(
                        "malformed correspondence, "
                                + "should be of the form "
                                + "conceptualvar1 = [exp_1]; ... conceptualvar_n = [exp_n]");
            }
            PSymbol eAsPSym = (PSymbol) e;
            PSymbol elhs = (PSymbol) eAsPSym.getArguments().get(0);
            PSymbol erhs = (PSymbol) eAsPSym.getArguments().get(1);
            start = start.substitute(elhs, erhs);
        }
        return start;
    }

    private List<PExp> getFormalParamConstraints(
            List<ResolveParser.ParameterDeclGroupContext> paramGrouping) {
        List<PExp> result = new ArrayList<>();
        for (ResolveParser.ParameterDeclGroupContext grp : paramGrouping) {
            //We actually don't give a shit about the individual terms. We
            //just care about the type of each term-grouping.
            PTType groupType = tr.progTypeValues.get(grp.type());
            result.add(getTypeLevelConstraint(groupType));
        }
        return result;
    }

    private PExp getTypeLevelConstraint(PTType t) {
        PExp result = g.getTrueExp();
        if (t instanceof PTFamily) {
            result = ((PTFamily) t).getConstraint();
        }
        else if (t instanceof PTRepresentation) {
            try {
                result = ((PTRepresentation) t).getFamily()
                        .getProgramType().getInitializationEnsures();
            } catch (NoneProvidedException e) {
                //No fam? How sad. But we'll manage; we'll just be true then
            }
        }
        return result;
    }

    private List<PExp> getModuleLevelAssertionsOfType(
            Predicate<Symbol> assertionType) {
        List<PExp> result = new ArrayList<>();
        for (String relatedScope : moduleScope.getRelatedModules()) {
            List<GlobalMathAssertionSymbol> intermediates =
                    symtab.moduleScopes.get(relatedScope)
                            .getSymbolsOfType(GlobalMathAssertionSymbol.class)
                            .stream().filter(assertionType)
                            .collect(Collectors.toList());

            result.addAll(intermediates.stream()
                    .map(GlobalMathAssertionSymbol::getEnclosedExp)
                    .collect(Collectors.toList()));
        }
        return result;
    }
}
