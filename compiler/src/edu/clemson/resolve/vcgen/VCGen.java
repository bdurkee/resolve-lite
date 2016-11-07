package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.semantics.*;
import edu.clemson.resolve.semantics.programtype.PTRepresentation;
import edu.clemson.resolve.semantics.programtype.ProgFamilyType;
import edu.clemson.resolve.semantics.programtype.ProgNamedType;
import edu.clemson.resolve.semantics.query.OperationQuery;
import edu.clemson.resolve.semantics.query.SymbolTypeQuery;
import edu.clemson.resolve.semantics.symbol.*;
import edu.clemson.resolve.semantics.symbol.GlobalMathAssertionSymbol.ClauseType;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol.ParameterMode;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.*;
import edu.clemson.resolve.vcgen.stats.*;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

//Note: This is the newer one..
public class VCGen extends ResolveBaseListener {

    private final AnnotatedModule tr;
    private final MathSymbolTable symtab;
    private final DumbMathClssftnHandler g;

    public static final VCStatRuleApplicationStrategy<VCSwap> SWAP_APPLICATION = new SwapApplicationStrategy();
    //public static final VCStatRuleApplicationStrategy<VCWhile> WHILE_APPLICATION = new WhileApplicationStrategy();
    //public static final VCStatRuleApplicationStrategy<VCIfElse> IF_ELSE_APPLICATION = new IfElseApplicationStrategy();

    //public static final VCStatRuleApplicationStrategy<VCCall> EXPLICIT_CALL_APPLICATION =
    //        new ExplicitCallApplicationStrategy();
    //public static final VCStatRuleApplicationStrategy<VCCall> GENERAL_CALL_APPLICATION =
    //        new GeneralCallApplicationStrategy();
    public static final VCStatRuleApplicationStrategy<VCAssign> FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();

    /** A mapping from facility name to function that maps facility formal parameter names to their actuals. */
    private final Map<String, Map<PExp, PExp>> facilitySpecFormalActualMappings = new HashMap<>();
    private final ParseTreeProperty<VCRuleBackedStat> stats = new ParseTreeProperty<>();
    private final VCOutputFile outputFile;
    private ModuleScopeBuilder moduleScope = null;

    private ProgReprTypeSymbol currentTypeReprSym = null;
    private final RESOLVECompiler compiler;

    public VCGen(RESOLVECompiler compiler, AnnotatedModule module) {
        this.symtab = compiler.symbolTable;
        this.tr = module;
        this.g = symtab.getTypeGraph();
        this.outputFile = new VCOutputFile(compiler);
        this.compiler = compiler;
    }

    public VCOutputFile getOutputFile() {
        return outputFile;
    }

    @Override
    public void enterModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        try {
            moduleScope = symtab.getModuleScope(tr.getModuleIdentifier());
        } catch (NoSuchModuleException e) {//shouldn't happen, but eh.
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE, Utils.getModuleCtxName(ctx));
        }
    }

    private List<PExp> reduceArgs(VCAssertiveBlockBuilder b, List<PExp> args) {
        List<PExp> result = new ArrayList<>();
        for (PExp progArg : args) {
            if (progArg instanceof PApply) { //i.e., we're dealing with a program call
                FunctionAssignApplicationStrategy.Invk_Cond ivkCondListener =
                        new FunctionAssignApplicationStrategy.Invk_Cond(b.definingTree, b);
                progArg.accept(ivkCondListener);
                result.add(ivkCondListener.mathFor(progArg));
            }
            else {
                result.add(progArg);
            }
        }
        return result;
    }

    public List<ModuleParameterSymbol> getAllModuleParameterSyms() {
        List<ModuleParameterSymbol> result = moduleScope.getSymbolsOfType(ModuleParameterSymbol.class);
        for (ModuleIdentifier e : moduleScope.getInheritedIdentifiers()) {
            try {
                ModuleScopeBuilder s = symtab.getModuleScope(e);
                result.addAll(s.getSymbolsOfType(ModuleParameterSymbol.class));
            } catch (NoSuchModuleException e1) { //should've been caught a long time ago
                throw new RuntimeException(e1);
            }
        }
        return result;
    }

    @Override
    public void enterProcedureDecl(ResolveParser.ProcedureDeclContext ctx) {
        Scope s = symtab.getScope(ctx);
        OperationSymbol op = null;
        VCAssertiveBlockBuilder block = null;
        try {
            List<ProgParameterSymbol> paramSyms = s.getSymbolsOfType(ProgParameterSymbol.class);

            op = s.queryForOne(new OperationQuery(null, ctx.name,
                    Utils.apply(paramSyms, ProgParameterSymbol::getDeclaredType)));

            //This is the requires for the operation with some substutions made (see corrFnExp rule in HH-diss)
            PExp corrFnExpRequires = perParameterCorrFnExpSubstitute(paramSyms, op.getRequires());
            List<PExp> opParamAntecedents = new ArrayList<>();
            Utils.apply(paramSyms, opParamAntecedents, this::extractAssumptionsFromParameter);
            block = new VCAssertiveBlockBuilder(g, s,
                        "Correct_Op_Hypo=" + ctx.name.getText(), ctx)
                        .facilitySpecializations(facilitySpecFormalActualMappings)
                        .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        //TODO: constraints should be added on demand via NOTICE:...
                        //.assume(getModuleLevelAssertionsOfType(ClauseType.CONSTRAINT))
                        .assume(opParamAntecedents) //we assume correspondence for reprs here automatically
                        .assume(corrFnExpRequires)
                        .remember();
        } catch (SymbolTableException e) {
            return; //shouldn't happen (we wouldn't have gotten here if it did)..
        }
        Scope scope = symtab.getScope(ctx);
        List<ProgParameterSymbol> paramSyms = scope.getSymbolsOfType(ProgParameterSymbol.class);
        List<ProgParameterSymbol> formalParameters = new ArrayList<>();
        try {
            formalParameters = scope.query(new SymbolTypeQuery<ProgParameterSymbol>(ProgParameterSymbol.class));
        } catch (NoSuchModuleException | UnexpectedSymbolException e) {
            e.printStackTrace();
        }

        List<PExp> corrFnExps = paramSyms.stream()
                .filter(p -> p.getDeclaredType() instanceof PTRepresentation)
                .map(p -> (PTRepresentation) p.getDeclaredType())
                .map(p -> p.getReprTypeSymbol().getCorrespondence())
                .collect(Collectors.toList());
        PExp corrFnExpEnsures = perParameterCorrFnExpSubstitute(paramSyms, op.getEnsures())
                .withVCInfo(ctx.getStart(), "Ensures clause of " + ctx.name.getText());
        //postcondition[params 1..i <-- corr_fn_exp]
        List<PExp> paramConsequents = new ArrayList<>();
        Utils.apply(formalParameters, paramConsequents, this::extractConsequentsFromParameter);
        /*block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
                .assume(corrFnExps)
                .confirm(ctx, g.formConjuncts(paramConsequents))
                .finalConfirm(corrFnExpEnsures);*/
        StmtListener l = new StmtListener(block, tr.exprASTs);
        ParseTreeWalker.DEFAULT.walk(l, ctx);
        List<VCRuleBackedStat> x = Utils.collect(VCRuleBackedStat.class, ctx.stmt(), l.stats);
        block.stats(x);
        block.finalConfirm(corrFnExpEnsures);
        outputFile.addAssertiveBlocks(block.build());
    }

    private List<PExp> getAssertionsFromModuleFormalParameters(List<ModuleParameterSymbol> parameters,
                                                               Function<ProgParameterSymbol, List<PExp>> extract) {
        List<PExp> result = new ArrayList<>();
        for (ModuleParameterSymbol p : parameters) {
            //todo: For now.
            if (p.getWrappedParamSymbol() instanceof ProgParameterSymbol) {
                result.addAll(extract.apply((ProgParameterSymbol) p.getWrappedParamSymbol()));
            }
        }
        return result;
    }

    private List<PExp> getAssertionsFromFormalParameters(List<ProgParameterSymbol> parameters,
                                                         Function<ProgParameterSymbol, List<PExp>> extract) {
        List<PExp> result = new ArrayList<>();
        for (ProgParameterSymbol p : parameters) {
            result.addAll(extract.apply(p));
        }
        return result;
    }

    private List<PExp> extractAssumptionsFromParameter(ProgParameterSymbol p) {
        List<PExp> resultingAssumptions = new ArrayList<>();
        if (p.getDeclaredType() instanceof ProgNamedType) {

            //both PTFamily AND PTRepresentation are a PTNamed
            ProgNamedType declaredType = (ProgNamedType) p.getDeclaredType();
            PExp exemplar = declaredType.getExemplarAsPSymbol();
            if (declaredType instanceof ProgFamilyType) {
                PExp constraint = ((ProgFamilyType) declaredType).getConstraint();
                constraint = constraint.substitute(
                        getSpecializationsForFacility(p.getTypeQualifier()));
                resultingAssumptions.add(constraint.substitute(
                        declaredType.getExemplarAsPSymbol(), p.asPSymbol())); // ASSUME TC (type constraint -- since we're conceptual)
            }
            else if (declaredType instanceof PTRepresentation) {
                ProgReprTypeSymbol repr = ((PTRepresentation) declaredType).getReprTypeSymbol();
                if (repr == null) return resultingAssumptions;
                PExp convention = repr.getConvention();
                resultingAssumptions.add(convention.substitute(declaredType.getExemplarAsPSymbol(), p.asPSymbol()));
                // ASSUME RC (repr convention -- since we're a repr)
                resultingAssumptions.add(repr.getCorrespondence());
            }
        }
        else { //PTGeneric
            //    resultingAssumptions.add(g.formInitializationPredicate(
            //            p.getDeclaredType(), p.getName()));
        }
        return resultingAssumptions;
    }

    private List<PExp> extractConsequentsFromParameter(ProgParameterSymbol p) {
        List<PExp> result = new ArrayList<>();
        PExp incParamExp = new PSymbolBuilder(p.asPSymbol()).incoming(true).build();
        PExp paramExp = new PSymbolBuilder(p.asPSymbol()).incoming(false).build();

        if (p.getDeclaredType() instanceof ProgNamedType) {
            ProgNamedType t = (ProgNamedType) p.getDeclaredType();
            PExp exemplar = new PSymbolBuilder(t.getExemplarName()).mathClssfctn(t.toMath()).build();

            if (t instanceof PTRepresentation) {
                ProgReprTypeSymbol repr = ((PTRepresentation) t).getReprTypeSymbol();

                PExp convention = repr.getConvention();
                PExp corrFnExp = repr.getCorrespondence();
                result.add(convention.substitute(t.getExemplarAsPSymbol(), paramExp)
                        .withVCInfo(p.getDefiningTree().getStart(), "Convention for type " +
                                t.getName()));
            }
            if (p.getMode() == ParameterMode.PRESERVES || p.getMode() == ParameterMode.RESTORES) {
                PExp equalsExp = g.formEquals(paramExp, incParamExp)
                        .withVCInfo(p.getDefiningTree().getStart(), "Ensure parameter " +
                                p.getName() + " is restored");
                result.add(equalsExp);
            }
            else if (p.getMode() == ParameterMode.CLEARS) {
                PExp init = ((ProgNamedType) p.getDeclaredType())
                        .getInitializationEnsures()
                        .substitute(exemplar, paramExp);
                result.add(init);
            }
        }
        return result;
    }

    private Set<PExp> getModuleLevelAssertionsOfType(ClauseType type) {
        Set<PExp> result = new LinkedHashSet<>();
        List<GlobalMathAssertionSymbol> assertions = new LinkedList<>();
        List<FacilitySymbol> facilities = new LinkedList<>();
        try {
            assertions.addAll(moduleScope.query(
                    new SymbolTypeQuery<GlobalMathAssertionSymbol>(GlobalMathAssertionSymbol.class))
                    .stream()
                    .filter(e -> e.getClauseType() == type)
                    .collect(Collectors.toList()));
            facilities.addAll(moduleScope.query(new SymbolTypeQuery<FacilitySymbol>(FacilitySymbol.class)));
        } catch (NoSuchModuleException | UnexpectedSymbolException e) {
        }
        return assertions.stream()
                .map(assertion -> substituteGlobalAssertionByAppropriateFacility(facilities, assertion))
                .collect(Collectors.toSet());
    }

    private PExp substituteGlobalAssertionByAppropriateFacility(List<FacilitySymbol> facilities,
                                                                GlobalMathAssertionSymbol e) {
        for (FacilitySymbol facility : facilities) {
            if (facility.getFacility().getSpecification()
                    .getModuleIdentifier().equals(e.getModuleIdentifier())) {
                return e.getEnclosedExp().substitute(
                        getSpecializationsForFacility(facility.getName()));
            }
        }
        return e.getEnclosedExp();
    }

    private Map<PExp, PExp> getSpecializationsForFacility(@Nullable String facility) {
        Map<PExp, PExp> result = facilitySpecFormalActualMappings.get(facility);
        if (result == null) result = new HashMap<>();
        return result;
    }

    //The only way I'm current aware of a local requires clause getting changed
    //is by passing a locally defined type  to an operation (something of type
    //PTRepresentation). This method won't do anything otherwise.
    @NotNull
    private PExp perParameterCorrFnExpSubstitute(@NotNull List<ProgParameterSymbol> params,
                                                 @Nullable PExp requiresOrEnsures) {
        List<PExp> result = new ArrayList<>();
        PExp resultingClause = requiresOrEnsures;
        for (ProgParameterSymbol p : params) {
            if (p.getDeclaredType() instanceof PTRepresentation) {
                ProgReprTypeSymbol repr = ((PTRepresentation) p.getDeclaredType()).getReprTypeSymbol();
                PExp corrFnExp = repr.getCorrespondence();
                //distribute conc.X into the clause passed
                Map<PExp, PExp> concReplMapping = new HashMap<>();
                concReplMapping.put(repr.exemplarAsPSymbol(), repr.conceptualExemplarAsPSymbol());
                concReplMapping.put(repr.exemplarAsPSymbol(true), repr.conceptualExemplarAsPSymbol(true));
                resultingClause = resultingClause.substitute(concReplMapping);
            }
        }
        return resultingClause == null ? g.getTrueExp() : resultingClause;
    }

    //-----------------------------------------------
    // S T A T S
    //-----------------------------------------------

    private static class StmtListener extends ResolveBaseListener {

        final ParseTreeProperty<VCRuleBackedStat> stats = new ParseTreeProperty<>();
        final VCAssertiveBlockBuilder builder;
        final ParseTreeProperty<PExp> asts;

        StmtListener(VCAssertiveBlockBuilder activeBuilder, ParseTreeProperty<PExp> asts) {
            this.builder = activeBuilder;
            this.asts = asts;
        }

        @Override
        public void exitStmt(ResolveParser.StmtContext ctx) {
            stats.put(ctx, stats.get(ctx.getChild(0)));
        }

        @Override
        public void exitCallStmt(ResolveParser.CallStmtContext ctx) {
            //VCCall s = new VCCall(ctx, builder, GENERAL_CALL_APPLICATION, (PApply) asts.get(ctx.progParamExp()));
            //stats.put(ctx, s);
            throw new UnsupportedOperationException("call not yet implemented");
        }

        @Override
        public void exitAssignStmt(ResolveParser.AssignStmtContext ctx) {
            PExp left = asts.get(ctx.left);
            PExp right = asts.get(ctx.right);
            VCAssign s = new VCAssign(ctx, builder, FUNCTION_ASSIGN_APPLICATION, left, right);
            stats.put(ctx, s);
        }

        @Override
        public void exitIfStmt(ResolveParser.IfStmtContext ctx) {
            /*PExp progCondition = asts.get(ctx.progExp());
            List<VCRuleBackedStat> thenStmts = Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats);
            List<VCRuleBackedStat> elseStmts = ctx.elseStmt() != null ?
                    Utils.collect(VCRuleBackedStat.class, ctx.elseStmt().stmt(), stats) : new ArrayList<>();
            VCIfElse s = new VCIfElse(ctx, builder, thenStmts, elseStmts, progCondition);
            stats.put(ctx, s);*/
            throw new UnsupportedOperationException("if-else not yet implemented");
        }

        @Override
        public void exitWhileStmt(ResolveParser.WhileStmtContext ctx) {
            /*PExp progCondition = asts.get(ctx.progExp());
            PExp maintainingClause = asts.get(ctx.maintainingClause().mathAssertionExp());
            PExp decreasingClause = ctx.decreasingClause() != null ? asts.get(ctx.decreasingClause().mathExp()) : null;
            List<VCRuleBackedStat> body = Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats);

            //now collect changing vars..
            Set<PSymbol> changing = new LinkedHashSet<>();
            if (ctx.changingClause() != null) {
                for (ResolveParser.MathExpContext e : ctx.changingClause().mathExp()) {
                    changing.add((PSymbol) asts.get(e));
                }
            }
            VCWhile s = new VCWhile(ctx, builder, WHILE_APPLICATION, progCondition,
                    maintainingClause, decreasingClause, changing, body);
            stats.put(ctx, s);*/
            throw new UnsupportedOperationException("while not yet implemented");
        }

        @Override
        public void exitSwapStmt(ResolveParser.SwapStmtContext ctx) {
            VCSwap s = new VCSwap(ctx, builder, SWAP_APPLICATION, asts.get(ctx.left), asts.get(ctx.right));
            stats.put(ctx, s);
        }
    }

    //TODO: TEST THIS
    public static boolean inSimpleForm(@NotNull PExp ensures, @NotNull List<ProgParameterSymbol> params) {
        boolean simple = false;
       /* if (ensures instanceof PApply) {
            PApply ensuresAsPApply = (PApply) ensures;
            List<PExp> args = ensuresAsPApply.getArguments();
            if (ensuresAsPApply.isEquality()) {
                if (inSimpleForm(args.get(0), params)) simple = true;
            }
            else if (ensuresAsPApply.isConjunct()) {
                if (inSimpleForm(args.get(0), params) && inSimpleForm(args.get(1), params)) simple = true;
            }
        }
        else if (ensures instanceof PSymbol) {
            for (ProgParameterSymbol p : params) {
                if (p.getMode() == ParameterMode.UPDATES && p.asPSymbol().equals(ensures)) simple = true;
            }
        }*/
        return simple;
    }

    /** "Next Prime Variable" */
    public static PSymbol NPV(PExp RP, PSymbol oldSym) {
        //Add an extra question mark to the front of oldSym
        PSymbol newOldSym = new PSymbol.PSymbolBuilder(oldSym, oldSym.getName() + "′").build();

        //Applies the question mark to oldSym if it is our first time visiting.
        if (RP.containsName(oldSym.getName())) {
            return NPV(RP, newOldSym);
        }
        //Don't need to apply the question mark here.
        else if (RP.containsName(newOldSym.getName())) {
            return NPV(RP, newOldSym);
        }
        else {
            //Return the new variable expression with the question mark
            int i = oldSym.getName().length() - 1;
            if (oldSym.getName().charAt(i) != '′') {
                return newOldSym;
            }
        }
        return oldSym;
    }

}