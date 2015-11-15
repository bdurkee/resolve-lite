package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.application.*;
import edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy.ExplicitCallRuleApplyingListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.TypeGraph;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.PTFamily;
import org.rsrg.semantics.programtype.PTNamed;
import org.rsrg.semantics.programtype.PTRepresentation;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.OperationQuery;
import org.rsrg.semantics.query.SymbolTypeQuery;
import org.rsrg.semantics.query.UnqualifiedNameQuery;
import org.rsrg.semantics.symbol.*;
import org.rsrg.semantics.symbol.GlobalMathAssertionSymbol.ClauseType;
import org.rsrg.semantics.symbol.ProgParameterSymbol.ParameterMode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy.getOperation;

public class ModelBuilderProto extends ResolveBaseListener {
    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final TypeGraph g;

    //TODO: in applyCallRule() in ModelBuilderProto, we should be going through
    //one of these static fields to apply the rule, we should make a class that
    //extends VCRuleBackedStat called VCCall which simply wraps a PExp representing
    //a call.
    public static final StatRuleApplicationStrategy<VCRuleBackedStat> EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    public static final StatRuleApplicationStrategy<VCRuleBackedStat> GENERAL_CALL_APPLICATION =
            new GeneralCallApplicationStrategy();
    //TODO:
    //Also have VCFuncAssign extends VCruleBackedStat, then you can have fields
    //which do things like getLhs(), getCall(), etc. That'd be nicer than doing
    //stats.getComponents().get(0), etc.
    private final static StatRuleApplicationStrategy<VCRuleBackedStat> FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();
    private final static StatRuleApplicationStrategy<VCRuleBackedStat> SWAP_APPLICATION =
            new SwapApplicationStrategy();

    private final Map<String, Map<PExp, PExp>> facilitySpecFormalActualMappings =
            new HashMap<>();
    private final ParseTreeProperty<VCRuleBackedStat> stats =
            new ParseTreeProperty<>();
    private final VCOutputFile outputFile = new VCOutputFile();
    private ModuleScopeBuilder moduleScope = null;

    private ProgReprTypeSymbol currentTypeReprSym = null;

    private final Deque<VCAssertiveBlockBuilder> assertiveBlocks =
            new LinkedList<>();

    private OperationSymbol currentProcOpSym = null;
    private boolean withinCallStmt = false;

    public ModelBuilderProto(VCGenerator gen, SymbolTable symtab) {
        this.symtab = symtab;
        this.tr = gen.getModule();
        this.g = symtab.getTypeGraph();
    }

    public VCOutputFile getOutputFile() {
        return outputFile;
    }

    @Override public void enterModule(ResolveParser.ModuleContext ctx) {
        moduleScope = symtab.moduleScopes.get(Utils.getModuleName(ctx));
    }

    @Override public void enterFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, moduleScope,
                        "Facility_Inst=" + ctx.name.getText(), ctx);
        block.assume(g.getTrueExp());
        assertiveBlocks.push(block);
    }

    @Override public void exitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {

        ModuleScopeBuilder spec = symtab.moduleScopes.get(ctx.spec.getText());
        ModuleScopeBuilder impl = symtab.moduleScopes.get(ctx.impl.getText());
        List<PExp> specArgs = ctx.specArgs.moduleArgument().stream()
                .map(tr.mathPExps::get).collect(Collectors.toList());
        List<PExp> reducedSpecArgs = reduceArgs(specArgs);

        List<PExp> formalSpecArgs = spec.getSymbolsOfType(ProgParameterSymbol.class)
                .stream().map(ProgParameterSymbol::asPSymbol)
                .collect(Collectors.toList());

        Map<PExp, PExp> specFormalsToActuals = Utils.zip(formalSpecArgs, reducedSpecArgs);
        facilitySpecFormalActualMappings.put(ctx.name.getText(), specFormalsToActuals);

        Optional<PExp> specReq = spec.getSymbolsOfType(GlobalMathAssertionSymbol.class)
                .stream().filter(e -> e.getClauseType() ==
                        ClauseType.REQUIRES)
                .map(GlobalMathAssertionSymbol::getEnclosedExp).findAny();

        PExp result = g.getTrueExp();
        if (specReq.isPresent()) {
            result = specReq.get();
        }
        if (ctx.externally == null) {
            Optional<PExp> implReq = impl.getSymbolsOfType(GlobalMathAssertionSymbol.class)
                    .stream().filter(e -> e.getClauseType() ==
                            ClauseType.REQUIRES)
                    .map(GlobalMathAssertionSymbol::getEnclosedExp).findAny();

            List<PExp> implArgs = ctx.implArgs.moduleArgument().stream()
                    .map(tr.mathPExps::get).collect(Collectors.toList());
            List<PExp> reducedImplArgs = reduceArgs(implArgs);

            List<PExp> formalImplArgs = spec.getSymbolsOfType(ProgParameterSymbol.class)
                    .stream().map(ProgParameterSymbol::asPSymbol)
                    .collect(Collectors.toList());
            Map<PExp, PExp> implFormalsToActuals = Utils.zip(formalImplArgs, reducedImplArgs);

            if (implReq.isPresent()) {
                //RPC[rn ~> rn_exp, RR ~> IRR]
                PExp RPC = implReq.get().substitute(implFormalsToActuals);

                //(RPC[rn ~> rn_exp, RR ~> IRR] /\ SpecRequires)
                result = g.formConjunct(RPC, result);
            }
        }
        //(RPC[rn ~> rn_exp, RR ~> IRR] /\ SpecRequires)[n ~> n_exp, r ~> IR]
        result = result.substitute(specFormalsToActuals);
        if (!result.isObviouslyTrue()) {
            assertiveBlocks.peek().finalConfirm(result);
        }
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        outputFile.addAssertiveBlock(block.build());
    }


    private List<PExp> reduceArgs(List<PExp> args) {
        List<PExp> result = new ArrayList<>();
        for (PExp arg : args) {
            if (arg.isFunctionApplication()) {
                PExp e = applyCallRuleToExp(assertiveBlocks.peek(), arg);
                result.add(e);
            } else {
                result.add(arg);
            }
        }
        return result;
    }

    private PExp applyCallRuleToExp(VCAssertiveBlockBuilder block, PExp exp) {
        ExplicitCallRuleApplyingListener applier =
                new ExplicitCallRuleApplyingListener(block);
        exp.accept(applier);
        PExp finalConfirm = block.finalConfirm.getConfirmExp();
        block.finalConfirm(finalConfirm.substitute(applier.test));
        if (applier.test.isEmpty()) {
            throw new IllegalStateException("something's screwy: " +
                    "shouldn't of tried applying " +
                    "call rule to: " + exp.toString()+".. " +
                    "could happen too right now if there's no spec for the op");
        }
        return applier.test.get(exp);
    }

    @Override public void enterTypeRepresentationDecl(
            ResolveParser.TypeRepresentationDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        currentTypeReprSym = null;
        try {
            currentTypeReprSym =
                    moduleScope.queryForOne(new UnqualifiedNameQuery(
                            ctx.name.getText())).toProgReprTypeSymbol();
        } catch (NoSuchSymbolException|DuplicateSymbolException e) {
        }
        List<ProgParameterSymbol> moduleParamSyms = getAllModuleParameterSyms();
        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, s,
                        "Well_Def_Corr_Hyp=" + ctx.name.getText(), ctx)
                        .assume(getSequentsFromFormalParameters(moduleParamSyms,
                                this::extractAntecedentsFromParameter))
                        .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        .assume(currentTypeReprSym.getConvention());
        assertiveBlocks.push(block);
    }

    public List<ProgParameterSymbol> getAllModuleParameterSyms() {
        ParserRuleContext moduleCtx = moduleScope.getDefiningTree();
        List<ProgParameterSymbol> result = new ArrayList<>();
        List<String> modulesToSearch = new ArrayList<>();

        modulesToSearch.add(moduleScope.getModuleID());
       /* if (moduleCtx instanceof ResolveParser.ConceptImplModuleContext) {
            Resolve.ConceptImplModuleContext moduleCtxAsConceptImpl =
                    (Resolve.ConceptImplModuleContext)moduleCtx;
            modulesToSearch.add(moduleCtxAsConceptImpl.concept.getText());
        }
        else */
        if (moduleCtx instanceof ResolveParser.ExtensionImplModuleContext) {
            ResolveParser.ExtensionImplModuleContext moduleCtxAsEnhImpl =
                    (ResolveParser.ExtensionImplModuleContext)moduleCtx;
            modulesToSearch.add(moduleCtxAsEnhImpl.concept.getText());
            modulesToSearch.add(moduleCtxAsEnhImpl.enhancement.getText());
        }
        for (String moduleName : modulesToSearch) {
            result.addAll(symtab.moduleScopes.get(moduleName)
                            .getSymbolsOfType(ProgParameterSymbol.class));
        }
        return result;
    }

   /* @Override public void exitTypeRepresentationDecl(
            Resolve.TypeRepresentationDeclContext ctx) {
        PExp constraint = g.getTrueExp();
        PExp correspondence = g.getTrueExp();
        if (currentTypeReprSym == null) return;
        correspondence = currentTypeReprSym.getCorrespondence();
        if ( currentTypeReprSym.getDefinition() != null ) {
            constraint = currentTypeReprSym.getDefinition()
                    .getProgramType().getConstraint();
        }
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        PExp newConstraint =
                constraint.substitute(currentTypeReprSym.exemplarAsPSymbol(),
                        currentTypeReprSym.conceptualExemplarAsPSymbol());
        //If the correspondence is multi-part, we split it; E.g.:
        //'conc.P.Trmnl_Loc' ~> 'SS(k)(P.Length, Cen(k))'
        //'conc.P.Curr_Loc' ~> 'SS(k)(P.Curr_Place, Cen(k))'
        //'conc.P.Lab' ~> \ 'q : Sp_Loc(k).({P.labl.Valu(SCD(q)) if SCD(q) + 1 <= P.Length; ...});'
        newConstraint = betaReduce(newConstraint, correspondence);
        block.assume(correspondence.splitIntoConjuncts());
        block.finalConfirm(newConstraint);
        outputFile.addAssertiveBlock(block.build());
    }

    @Override public void enterTypeImplInit(Resolve.TypeImplInitContext ctx) {
        Scope s = symtab.scopes.get(ctx.getParent());
        PExp convention = currentTypeReprSym.getConvention();
        PExp correspondence = currentTypeReprSym.getCorrespondence();
        PExp typeInitEnsures = g.getTrueExp();
        List<ProgParameterSymbol> moduleParamSyms = getAllModuleParameterSyms();

        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, s,
                    "T_Init_Hypo=" + currentTypeReprSym.getName(), ctx)
                    .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                    .assume(getSequentsFromFormalParameters(moduleParamSyms,
                            this::extractAntecedentsFromParameter));

        assertiveBlocks.push(block);
    }

    @Override public void exitTypeImplInit(Resolve.TypeImplInitContext ctx) {
        PExp typeInitEnsures = g.getTrueExp();
        PExp convention = currentTypeReprSym.getConvention();
        PExp correspondence = currentTypeReprSym.getCorrespondence();
        if ( currentTypeReprSym.getDefinition() != null ) {
            typeInitEnsures =
                    currentTypeReprSym.getDefinition().getProgramType()
                            .getInitializationEnsures();
        }
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        PExp newInitEnsures =
                typeInitEnsures.substitute(currentTypeReprSym.exemplarAsPSymbol(),
                        currentTypeReprSym.conceptualExemplarAsPSymbol());
        //newInitEnsures =
        //        betaReduce(newInitEnsures,
        //                correspondence);
        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats));
        block.confirm(convention);  //order here is imp.
        block.assume(correspondence);
        block.finalConfirm(newInitEnsures);
        outputFile.addAssertiveBlock(block.build());
    }*/

    @Override public void enterOperationProcedureDecl(
            ResolveParser.OperationProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        List<ProgParameterSymbol> paramSyms =
                s.getSymbolsOfType(ProgParameterSymbol.class);

        PExp corrFnExpRequires = perParameterCorrFnExpSubstitute(paramSyms,
              ctx, tr.getPExpFor(g, ctx.requiresClause())); //precondition[params 1..i <-- conc.X]

        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, s,
                        "Proc_Decl_rule="+ctx.name.getText(), ctx)
                        .facilitySpecializations(facilitySpecFormalActualMappings)
                        .assume(getSequentsFromFormalParameters(paramSyms,
                                this::extractAntecedentsFromParameter))
                        .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        .assume(getModuleLevelAssertionsOfType(ClauseType.CONSTRAINT))
                        .assume(corrFnExpRequires)
                        .remember();

        assertiveBlocks.push(block);
    }

    @Override public void exitOperationProcedureDecl(
            ResolveParser.OperationProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        List<ProgParameterSymbol> paramSyms =
                s.getSymbolsOfType(ProgParameterSymbol.class);

        PExp corrFnExpEnsures = perParameterCorrFnExpSubstitute(paramSyms,
                ctx, tr.getPExpFor(g, ctx.ensuresClause())); //postcondition[params 1..i <-- corr_fn_exp]
        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
                .confirm(getSequentsFromFormalParameters(
                        paramSyms, this::extractConsequentsFromParameter))
                //.assume(corrFnExps)
                .finalConfirm(corrFnExpEnsures);

        outputFile.addAssertiveBlock(block.build());
    }

    @Override public void enterProcedureDecl(
            ResolveParser.ProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        try {
            List<ProgParameterSymbol> paramSyms =
                    s.getSymbolsOfType(ProgParameterSymbol.class);
            currentProcOpSym = s.queryForOne(
                    new OperationQuery(null, ctx.name,
                            paramSyms.stream()
                                    .map(ProgParameterSymbol::getDeclaredType)
                                    .collect(Collectors.toList())));
            PExp corrFnExpRequires = perParameterCorrFnExpSubstitute(
                    paramSyms, ctx, currentProcOpSym.getRequires());

            VCAssertiveBlockBuilder block =
                    new VCAssertiveBlockBuilder(g, s,
                            "Correct_Op_Hypo="+ctx.name.getText(), ctx)
                            .facilitySpecializations(facilitySpecFormalActualMappings)
                            .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                            .assume(getModuleLevelAssertionsOfType(ClauseType.CONSTRAINT))
                            .assume(getSequentsFromFormalParameters(paramSyms,
                                    this::extractAntecedentsFromParameter)) //we assume correspondence for reprs here automatically
                            .assume(corrFnExpRequires)
                            .remember();
            assertiveBlocks.push(block);
        }
        catch (DuplicateSymbolException|NoSuchSymbolException e) {
            e.printStackTrace();    //shouldn't happen, we wouldn't be in vcgen if it did
        }
    }

    @Override public void exitProcedureDecl(
            ResolveParser.ProcedureDeclContext ctx) {
        Scope scope = symtab.scopes.get(ctx);
        List<ProgParameterSymbol> paramSyms =
                scope.getSymbolsOfType(ProgParameterSymbol.class);
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        List<ProgParameterSymbol> formalParameters = scope.query(
                new SymbolTypeQuery<ProgParameterSymbol>(
                        ProgParameterSymbol.class));

        List<PExp> corrFnExps = paramSyms.stream()
                .filter(p -> p.getDeclaredType() instanceof PTRepresentation)
                .map(p -> (PTRepresentation)p.getDeclaredType())
                .map(p -> p.getReprTypeSymbol().getCorrespondence())
                .collect(Collectors.toList());
        PExp corrFnExpEnsures = perParameterCorrFnExpSubstitute(paramSyms,
                ctx, currentProcOpSym.getEnsures()); //postcondition[params 1..i <-- corr_fn_exp]

        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
            .confirm(getSequentsFromFormalParameters(formalParameters,
                    this::extractConsequentsFromParameter)) //we assume correspondence for reprs here automatically
            .assume(corrFnExps)
            .finalConfirm(corrFnExpEnsures);

        outputFile.addAssertiveBlock(block.build());
        currentProcOpSym = null;
    }

    @Override public void exitVariableDeclGroup(
            ResolveParser.VariableDeclGroupContext ctx) {
        PTType type = tr.progTypeValues.get(ctx.type());
        MTType mathType = tr.mathTypeValues.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {

            if (type instanceof PTNamed) {
                PExp init = ((PTNamed)type).getInitializationEnsures();
                PSymbol v = new PSymbol.PSymbolBuilder(t.getText())
                        .mathType(mathType).progType(type).build();
                init = init.substitute(((PTNamed) type)
                        .getExemplarAsPSymbol(), v);
                assertiveBlocks.peek().assume(init);
            }
            else { //generic case
                assertiveBlocks.peek().assume(
                        g.formInitializationPredicate(type, t.getText()));
            }
        }
    }

    //-----------------------------------------------
    // S T A T S
    //-----------------------------------------------

    @Override public void exitStmt(ResolveParser.StmtContext ctx) {
        stats.put(ctx, stats.get(ctx.getChild(0)));
    }

    //TODO: TEST THIS
    private boolean inSimpleForm(@NotNull PExp ensures,
                                 @NotNull List<ProgParameterSymbol> params) {
        boolean simple = false;
        if (ensures instanceof PApply) {
            PApply ensuresAsPApply = (PApply)ensures;
            List<PExp> args = ensuresAsPApply.getArguments();
            if (ensuresAsPApply.isEquality()) {
                if (inSimpleForm(args.get(0), params)) simple = true;
            }
            else if (ensuresAsPApply.isConjunct()) {
                if (inSimpleForm(args.get(0), params) &&
                        inSimpleForm(args.get(1), params)) simple = true;
            }
        }
        else if (ensures instanceof PSymbol) {
            for (ProgParameterSymbol p : params) {
                if (p.getMode() == ParameterMode.UPDATES &&
                        p.asPSymbol().equals(ensures)) simple = true;
            }
        }
        return simple;
    }

    @Override public void exitCallStmt(ResolveParser.CallStmtContext ctx) {
        VCRuleBackedStat s = null;
        PApply callExp = (PApply)tr.mathPExps.get(ctx.progExp());
        OperationSymbol op = getOperation(moduleScope, callExp);
       /* if (inSimpleForm(op.getEnsures(), op.getParameters())) {
            symtab.getCompiler().info("APPLYING EXPLICIT (SIMPLE) CALL RULE");
            s = new VCRuleBackedStat(ctx, assertiveBlocks.peek(),
                    EXPLICIT_CALL_APPLICATION, callExp);
        }
        else {*/
            symtab.getCompiler().info("APPLYING GENERAL CALL RULE");
            s = new VCRuleBackedStat(ctx, assertiveBlocks.peek(),
                    GENERAL_CALL_APPLICATION, callExp);
        //}
        stats.put(ctx, s);
    }

    //if the immediate parent is a callStmtCtx then add an actual stmt for this guy,
    //otherwise,
    @Override public void exitSwapStmt(ResolveParser.SwapStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, assertiveBlocks.peek(),
                        SWAP_APPLICATION, tr.mathPExps.get(ctx.left),
                        tr.mathPExps.get(ctx.right));
        stats.put(ctx, s);
    }

    @Override public void exitAssignStmt(ResolveParser.AssignStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, assertiveBlocks.peek(),
                        FUNCTION_ASSIGN_APPLICATION,
                        tr.mathPExps.get(ctx.left),
                        tr.mathPExps.get(ctx.right));
        stats.put(ctx, s);
    }

    private List<PExp> getSequentsFromFormalParameters(
            List<ProgParameterSymbol> parameters,
            Function<ProgParameterSymbol, List<PExp>> extractionFunction) {
        List<PExp> result = new ArrayList<>();
        for (ProgParameterSymbol p : parameters) {
            result.addAll(extractionFunction.apply(p));
        }
        return result;
    }

    private List<PExp> extractAntecedentsFromParameter(ProgParameterSymbol p) {
        List<PExp> resultingAssumptions = new ArrayList<>();
        if ( p.getDeclaredType() instanceof PTNamed) {

            //both PTFamily AND PTRepresentation are a PTNamed
            PTNamed declaredType = (PTNamed)p.getDeclaredType();
            PExp exemplar = declaredType.getExemplarAsPSymbol();
            if (declaredType instanceof PTFamily) {
                PExp constraint = ((PTFamily) declaredType).getConstraint();

                constraint = constraint.substitute(
                        getSpecializationsForFacility(p.getTypeQualifier()));
                resultingAssumptions.add(constraint.substitute(
                        declaredType.getExemplarAsPSymbol(), p.asPSymbol())); // ASSUME TC (type constraint -- since we're conceptual)
            }
            else if (declaredType instanceof PTRepresentation)  {
                ProgReprTypeSymbol repr =
                        ((PTRepresentation) declaredType).getReprTypeSymbol();
                PExp convention = repr.getConvention();

                resultingAssumptions.add(convention.substitute(
                        declaredType.getExemplarAsPSymbol(), p.asPSymbol())); // ASSUME RC (repr convention -- since we're a repr)
                resultingAssumptions.add(repr.getCorrespondence());
            }
        }
        else { //PTGeneric
            resultingAssumptions.add(g.formInitializationPredicate(
                    p.getDeclaredType(), p.getName()));
        }
        return resultingAssumptions;
    }

    private List<PExp> extractConsequentsFromParameter(ProgParameterSymbol p) {
        List<PExp> result = new ArrayList<>();
        PExp incParamExp = new PSymbol.PSymbolBuilder(p.asPSymbol())
                .incoming(true).build();
        PExp paramExp = new PSymbol.PSymbolBuilder(p.asPSymbol())
                .incoming(false).build();

        if (p.getDeclaredType() instanceof PTNamed) {
            PTNamed t = (PTNamed) p.getDeclaredType();
            PExp exemplar =
                    new PSymbol.PSymbolBuilder(t.getExemplarName())
                            .mathType(t.toMath()).build();

            if (t instanceof PTRepresentation) {
                ProgReprTypeSymbol repr =
                        ((PTRepresentation) t).getReprTypeSymbol();

                PExp convention = repr.getConvention();
                PExp corrFnExp = repr.getCorrespondence();
                result.add(convention.substitute(t.getExemplarAsPSymbol(), paramExp));
            }
            if (p.getMode() == ParameterMode.PRESERVES
                    || p.getMode() == ParameterMode.RESTORES) {
                PExp equalsExp = g.formEquals(paramExp, incParamExp);
                result.add(equalsExp);
            }
            else if (p.getMode() == ParameterMode.CLEARS) {
                PExp init = ((PTNamed) p.getDeclaredType())
                        .getInitializationEnsures()
                        .substitute(exemplar, paramExp);
                result.add(init);
            }
        }
        return result;
    }

    private Set<PExp> getModuleLevelAssertionsOfType(ClauseType type) {
        Set<PExp> result = new LinkedHashSet<>();
        List<GlobalMathAssertionSymbol> assertions = moduleScope.query(
                new SymbolTypeQuery<GlobalMathAssertionSymbol>
                        (GlobalMathAssertionSymbol.class)).stream()
                        .filter(e -> e.getClauseType() == type)
                        .collect(Collectors.toList());

        List<FacilitySymbol> facilities = moduleScope.query(
                new SymbolTypeQuery<FacilitySymbol>(FacilitySymbol.class));
        return assertions.stream()
                .map(assertion -> substituteByFacilities(facilities, assertion))
                .collect(Collectors.toSet());
    }

    private PExp substituteByFacilities(List<FacilitySymbol> facilities,
                                        GlobalMathAssertionSymbol e) {
        for (FacilitySymbol facility : facilities) {
            if (facility.getFacility().getSpecification().getName()
                    .equals(e.getModuleID())) {
                return e.getEnclosedExp().substitute(
                        getSpecializationsForFacility(facility.getName()));
            }
        }
        return e.getEnclosedExp();
    }

    private Map<PExp, PExp> getSpecializationsForFacility(String facility) {
        Map<PExp, PExp> result = facilitySpecFormalActualMappings.get(facility);
        if (result == null) result = new HashMap<>();
        return result;
    }

    //The only way I'm current aware of a local requires clause getting changed
    //is by passing a locally defined type  to an operation (something of type
    //PTRepresentation). This method won't do anything otherwise.
    private PExp perParameterCorrFnExpSubstitute(List<ProgParameterSymbol> params,
                                                 ParserRuleContext functionCtx,
                                                 PExp requiresOrEnsures) {
        List<PExp> result = new ArrayList<>();
        PExp resultingClause = requiresOrEnsures;
        for (ProgParameterSymbol p : params) {
            if (p.getDeclaredType() instanceof PTRepresentation) {
                ProgReprTypeSymbol repr =
                        ((PTRepresentation) p.getDeclaredType()).getReprTypeSymbol();

                PExp corrFnExp = repr.getCorrespondence();
                //distribute conc.X into the clause passed
                Map<PExp, PExp> concReplMapping = new HashMap<>();
                concReplMapping.put(repr.exemplarAsPSymbol(),
                        repr.conceptualExemplarAsPSymbol());
                concReplMapping.put(repr.exemplarAsPSymbol(true),
                        repr.conceptualExemplarAsPSymbol(true));

                resultingClause = resultingClause.substitute(concReplMapping);
            }
        }
        return resultingClause;
    }
}