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
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.query.OperationQuery;
import edu.clemson.resolve.semantics.query.SymbolTypeQuery;
import edu.clemson.resolve.semantics.symbol.*;
import edu.clemson.resolve.semantics.symbol.GlobalMathAssertionSymbol.ClauseType;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol.ParameterMode;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.*;
import edu.clemson.resolve.vcgen.stats.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
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
    public static final VCStatRuleApplicationStrategy<VCWhile> WHILE_APPLICATION = new WhileApplicationStrategy();
    public static final VCStatRuleApplicationStrategy<VCIfElse> IF_ELSE_APPLICATION = new IfElseApplicationStrategy();

    public static final VCStatRuleApplicationStrategy<VCCall> GENERAL_CALL_APPLICATION =
            new GeneralCallApplicationStrategy();
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


    @Override
    public void enterFacilityDecl(ResolveParser.FacilityDeclContext ctx) {
        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, moduleScope,
                        "Facility_Inst=" + ctx.name.getText(), ctx);
        //block.assume(g.getTrueExp());

        ModuleScopeBuilder spec = null, impl = null;
        try {
            ModuleIdentifier concept = moduleScope.getImportWithName(ctx.spec);
            spec = symtab.getModuleScope(concept);
            if (ctx.externally == null) {
                ModuleIdentifier imp = moduleScope.getImportWithName(ctx.impl);
                impl = symtab.getModuleScope(imp);
            }
        } catch (NoSuchModuleException nsme) {
            return; //shouldn't happen...
        }

        List<ProgType> specArsgs = ctx.specArgs.progExp().stream()
                .map(e -> tr.progTypes.get(e))
                .collect(Collectors.toList());
        List<PExp> specArgs = ctx.specArgs.progExp().stream()
                .map(e -> tr.exprASTs.get(e))
                .collect(Collectors.toList());
        List<PExp> reducedSpecArgs = reduceArgs(block, specArgs);

        List<PExp> formalSpecArgs = spec.getSymbolsOfType(ModuleParameterSymbol.class).stream()
                .map(ModuleParameterSymbol::asPSymbol)
                .collect(Collectors.toList());

        Map<PExp, PExp> specFormalsToActuals = Utils.zip(formalSpecArgs, reducedSpecArgs);
        facilitySpecFormalActualMappings.put(ctx.name.getText(), specFormalsToActuals);

        Optional<PExp> specReq = spec.getSymbolsOfType(GlobalMathAssertionSymbol.class)
                .stream()
                .filter(e -> e.getClauseType() == ClauseType.REQUIRES)
                .map(GlobalMathAssertionSymbol::getEnclosedExp)
                .findAny();

        PExp result = specReq.isPresent() ? specReq.get() : g.getTrueExp();
        result = result.withVCInfo(ctx.getStart(), "Requires clause for concept: " + ctx.spec.getText()
                + " in facility instantiation rule");
        if (ctx.externally == null && impl != null && ctx.implArgs != null) {
            Optional<PExp> implReq = impl.getSymbolsOfType(GlobalMathAssertionSymbol.class)
                    .stream()
                    .filter(e -> e.getClauseType() == ClauseType.REQUIRES)
                    .map(GlobalMathAssertionSymbol::getEnclosedExp)
                    .findAny();
            List<PExp> implArgs = ctx.implArgs.progExp().stream().map(tr.exprASTs::get).collect(Collectors.toList());
            List<PExp> reducedImplArgs = reduceArgs(block, implArgs);
            List<PExp> formalImplArgs =
                    Utils.apply(spec.getSymbolsOfType(ProgParameterSymbol.class), ProgParameterSymbol::asPSymbol);
            Map<PExp, PExp> implFormalsToActuals = Utils.zip(formalImplArgs, reducedImplArgs);

            if (implReq.isPresent()) {
                //RPC[rn ~> rn_exp, RR ~> IRR]
                PExp RPC = implReq.get().substitute(implFormalsToActuals).withVCInfo(ctx.getStart(),
                        "Requires clause for realization: " + ctx.impl.getText() + " in facility instantiation rule");

                //(RPC[rn ~> rn_exp, RR ~> IRR] /\ SpecRequires)
                result = g.formConjunct(RPC, result);
            }
        }
        //(RPC[rn ~> rn_exp, RR ~> IRR] /\ CPC)[n ~> n_exp, r ~> IR]
        result = result.substitute(specFormalsToActuals);
        if (!result.isObviouslyTrue()) {
            block.finalConfirm(result);
            outputFile.addAssertiveBlocks(block.build());
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
    public void enterOperationProcedureDecl(ResolveParser.OperationProcedureDeclContext ctx) {
        Scope s = symtab.getScope(ctx);
        List<ProgParameterSymbol> paramSyms = s.getSymbolsOfType(ProgParameterSymbol.class);

        //precondition[params 1..i ~> conc.X]
        PExp corrFnExpRequires = perParameterCorrFnExpSubstitute(paramSyms,
                tr.getMathExpASTFor(g, ctx.requiresClause()));

        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, s,
                        "Proc_Decl_rule=" + ctx.name.getText(), ctx)
                        .facilitySpecializations(facilitySpecFormalActualMappings)
                        .assume(getAssertionsFromModuleFormalParameters(getAllModuleParameterSyms(), this::extractAssumptionsFromParameter))
                        .assume(getAssertionsFromFormalParameters(paramSyms, this::extractAssumptionsFromParameter))
                        //.assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        //.assume(getModuleLevelAssertionsOfType(ClauseType.CONSTRAINT))
                        .assume(corrFnExpRequires)
                        .remember();
        assumeVarDecls(ctx.varDeclGroup(), block);
        //stats
        StmtListener l = new StmtListener(block, tr.exprASTs);
        ParseTreeWalker.DEFAULT.walk(l, ctx);
        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), l.stats));

        PExp corrFnExpEnsures = perParameterCorrFnExpSubstitute(paramSyms,
                tr.getMathExpASTFor(g, ctx.ensuresClause())); //postcondition[params 1..i <-- corr_fn_exp]
        corrFnExpEnsures = corrFnExpEnsures.withVCInfo(ctx.getStart(), "Ensures clause of " + ctx.name.getText());
        Token loc = ctx.ensuresClause() != null ? ctx.ensuresClause().getStart() : ctx.getStart();

        List<PExp> paramConsequents = new ArrayList<>();
        Utils.apply(paramSyms, paramConsequents, this::extractConsequentsFromParameter);

        //add any additional confirms from the parameters, etc
        for (ProgParameterSymbol p : paramSyms) {
            confirmParameterConsequentsForBlock(block, p); //modfies 'block' with additional confims!
        }
        block.finalConfirm(corrFnExpEnsures);
        outputFile.addAssertiveBlocks(block.build());
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
                        //.assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        //TODO: constraints should be added on demand via NOTICE:...
                        //.assume(getModuleLevelAssertionsOfType(ClauseType.CONSTRAINT))
                        .assume(opParamAntecedents) //we assume correspondence for reprs here automatically
                        .assume(corrFnExpRequires)
                        .remember();
            //add in any user defined notices...
            for (ResolveParser.NoticeClauseContext notice : ctx.noticeClause()) {
                block.assume(tr.exprASTs.get(notice.mathExp()), false, true);
            }
            assumeVarDecls(ctx.varDeclGroup(), block);
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
        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), l.stats));

        block.finalConfirm(corrFnExpEnsures);
        outputFile.addAssertiveBlocks(block.build());
    }

    private void assumeVarDecls(List<ResolveParser.VarDeclGroupContext> group, VCAssertiveBlockBuilder builder) {
        for (ResolveParser.VarDeclGroupContext vars : group) {
            ProgType type = tr.progTypes.get(vars.type());
            for (TerminalNode t : vars.ID()) {
                PSymbol var = new PSymbol.PSymbolBuilder(t.getText())
                        .progType(type).mathClssfctn(type.toMath())
                        .build();
                if (type instanceof ProgNamedType) {
                    PSymbol exemplar = ((ProgNamedType) type).getExemplarAsPSymbol();
                    PExp init = ((ProgNamedType) type).getInitializationEnsures();
                    init = init.substitute(exemplar, var);
                    //substitute by the facility the type came through

                    //get the qualifier out of the concrete syntax.
                    if (vars.type() instanceof ResolveParser.NamedTypeContext) {
                        ResolveParser.NamedTypeContext namedTypeNode =
                                (ResolveParser.NamedTypeContext) vars.type();
                        if (namedTypeNode.qualifier != null) {
                            Map<PExp, PExp> facilitySubstitutions =
                                    builder.facilitySpecializations.get(namedTypeNode.qualifier.getText());
                            if (namedTypeNode.qualifier != null && facilitySubstitutions != null) {
                                init = init.substitute(facilitySubstitutions);
                            }
                        }
                    }
                    builder.assume(init);
                }
            }
        }
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

    private void confirmParameterConsequentsForBlock(VCAssertiveBlockBuilder block, ProgParameterSymbol p) {
        PExp incParamExp = new PSymbolBuilder(p.asPSymbol()).incoming(true).build();
        PExp paramExp = new PSymbolBuilder(p.asPSymbol()).incoming(false).build();

        if (p.getDeclaredType() instanceof ProgNamedType) {
            ProgNamedType t = (ProgNamedType) p.getDeclaredType();
            PExp exemplar = new PSymbolBuilder(t.getExemplarName()).mathClssfctn(t.toMath()).build();

            if (t instanceof PTRepresentation) {
                ProgReprTypeSymbol repr = ((PTRepresentation) t).getReprTypeSymbol();

                PExp convention = repr.getConvention();
                PExp corrFnExp = repr.getCorrespondence();
                //if we're doing this its going to be on a procedure decl or op-proc decl, so just
                //say block.definingTree
                PExp newConvention = convention.substitute(t.getExemplarAsPSymbol(), paramExp)
                        .withVCInfo(block.definingTree.getStart(), "Convention for " + t.getName());
                block.confirm(block.definingTree, newConvention);
            }
            if (p.getMode() == ParameterMode.PRESERVES || p.getMode() == ParameterMode.RESTORES) {
                PExp equalsExp = g.formEquals(paramExp, incParamExp)
                        .withVCInfo(block.definingTree.getStart(), "Ensure parameter " + p.getName() + " is restored");
                block.confirm(block.definingTree, equalsExp);
            }
            else if (p.getMode() == ParameterMode.CLEARS) {
                PExp init = ((ProgNamedType) p.getDeclaredType())
                        .getInitializationEnsures()
                        .substitute(exemplar, paramExp);
                //result.add(init);
            }
        }
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
            VCCall s = new VCCall(ctx, builder, GENERAL_CALL_APPLICATION, (PApply) asts.get(ctx.progParamExp()));
            stats.put(ctx, s);
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
            PExp progCondition = asts.get(ctx.progExp());
            List<VCRuleBackedStat> thenStmts = Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats);
            List<VCRuleBackedStat> elseStmts = ctx.elseStmt() != null ?
                    Utils.collect(VCRuleBackedStat.class, ctx.elseStmt().stmt(), stats) : new ArrayList<>();
            VCIfElse s = new VCIfElse(ctx, builder, thenStmts, elseStmts, progCondition);
            stats.put(ctx, s);
        }

        @Override
        public void exitWhileStmt(ResolveParser.WhileStmtContext ctx) {
            PExp progCondition = asts.get(ctx.progExp());
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
            stats.put(ctx, s);
        }

        @Override
        public void exitSwapStmt(ResolveParser.SwapStmtContext ctx) {
            VCSwap s = new VCSwap(ctx, builder, SWAP_APPLICATION, asts.get(ctx.left), asts.get(ctx.right));
            stats.put(ctx, s);
        }
    }

    /** "Next Prime Variable" (over sequents) */
    public static PSymbol NPV(Collection<Sequent> sequents, PSymbol oldSym) {
        PSymbol result = oldSym;

        for (Sequent sequent : sequents) {
            for (PExp formula : sequent.getLeftFormulas()) {
                PSymbol temp = NPV(formula, oldSym);
                if (temp.getName().length() > result.getName().length()) {
                    result = temp;
                }
            }
            for (PExp formula : sequent.getRightFormulas()) {
                PSymbol temp = NPV(formula, oldSym);
                if (temp.getName().length() > result.getName().length()) {
                    result = temp;
                }
            }
        }
        return result;
    }

    /** "Next Prime Variable" */
    private static PSymbol NPV(PExp wff, PSymbol oldSym) {
        //Add an extra question mark to the front of oldSym
        PSymbol newOldSym = new PSymbol.PSymbolBuilder(oldSym, oldSym.getName() + "′").build();

        //Primes oldSym if it is our first time visiting.
        if (wff.containsName(oldSym.getName())) {
            return NPV(wff, newOldSym);
        }
        //Don't need to prime here
        else if (wff.containsName(newOldSym.getName())) {
            return NPV(wff, newOldSym);
        }
        else {
            //Return the new variable expression with the prime
            int i = oldSym.getName().length() - 1;
            if (oldSym.getName().charAt(i) != '′') {
                return newOldSym;
            }
        }
        return oldSym;
    }

}