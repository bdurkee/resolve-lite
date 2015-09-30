package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.application.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
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

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ModelBuilderProto extends ResolveBaseListener {
    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final TypeGraph g;

    public static final StatRuleApplicationStrategy<VCRuleBackedStat> EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
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

    @Override public void enterModule(Resolve.ModuleContext ctx) {
        moduleScope = symtab.moduleScopes.get(Utils.getModuleName(ctx));
    }

    @Override public void enterFacilityDecl(Resolve.FacilityDeclContext ctx) {
        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, moduleScope,
                        "Facility_Inst=" + ctx.name.getText(), ctx);
        block.assume(g.getTrueExp());
        assertiveBlocks.push(block);
    }

    @Override public void exitFacilityDecl(Resolve.FacilityDeclContext ctx) {

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
                .stream().filter(e -> e.getClauseType() == ClauseType.REQUIRES)
                .map(GlobalMathAssertionSymbol::getEnclosedExp).findAny();

        PExp result = g.getTrueExp();
        if (specReq.isPresent()) {
            result = specReq.get();
        }
        if (ctx.externally == null) {
            Optional<PExp> implReq = impl.getSymbolsOfType(GlobalMathAssertionSymbol.class)
                    .stream().filter(e -> e.getClauseType() == ClauseType.REQUIRES)
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

    /** Applies simple call rule to any arguments in {@code args} that need
     *  it (e.g.: they're arithmetic expressions)
     */
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
        PExpSomethingListener something = new PExpSomethingListener(block);
        exp.accept(something);
        PExp finalConfirm = block.finalConfirm.getConfirmExp();
        block.finalConfirm(finalConfirm.substitute(something.test));
        if (something.test.isEmpty()) {
            throw new IllegalStateException("something's screwy: " +
                    "shouldn't of tried applying " +
                    "call rule to: " + exp.toString()+".. " +
                    "could happen too right now if there's no spec for the op");
        }
        return something.test.get(exp);
    }

    @Override public void enterTypeRepresentationDecl(
            Resolve.TypeRepresentationDeclContext ctx) {
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
        if (moduleCtx instanceof Resolve.ConceptImplModuleContext) {
            Resolve.ConceptImplModuleContext moduleCtxAsConceptImpl =
                    (Resolve.ConceptImplModuleContext)moduleCtx;
            modulesToSearch.add(moduleCtxAsConceptImpl.concept.getText());
        } //todo: enhancement impl module -- should be 'concept' and 'enhancement' for search
        for (String moduleName : modulesToSearch) {
            result.addAll(symtab.moduleScopes.get(moduleName)
                            .getSymbolsOfType(ProgParameterSymbol.class));
        }
        return result;
    }

    @Override public void exitTypeRepresentationDecl(
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
    }

    @Override public void enterOperationProcedureDecl(
            Resolve.OperationProcedureDeclContext ctx) {
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
                        //.assume(corrFnExpsForParams)
                        .assume(corrFnExpRequires)
                        .remember();

        assertiveBlocks.push(block);
    }

    @Override public void exitOperationProcedureDecl(
            Resolve.OperationProcedureDeclContext ctx) {
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

    @Override public void enterProcedureDecl(Resolve.ProcedureDeclContext ctx) {
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

    @Override public void exitProcedureDecl(Resolve.ProcedureDeclContext ctx) {
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
            Resolve.VariableDeclGroupContext ctx) {
        PTType type = tr.progTypeValues.get(ctx.type());
        MTType mathType = tr.mathTypeValues.get(ctx.type());
        if (type instanceof PTNamed) {
            PExp init = ((PTNamed)type).getInitializationEnsures();
            for (TerminalNode t : ctx.ID()) {
                PSymbol v = new PSymbol.PSymbolBuilder(t.getText())
                        .mathType(mathType).progType(type).build();
                init = init.substitute(((PTNamed) type)
                        .getExemplarAsPSymbol(), v);
                assertiveBlocks.peek().assume(init);
            }
        } else { //generic

        }
    }

    //-----------------------------------------------
    // S T A T S
    //-----------------------------------------------

    @Override public void exitStmt(Resolve.StmtContext ctx) {
        stats.put(ctx, stats.get(ctx.getChild(0)));
    }

    @Override public void exitCallStmt(Resolve.CallStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, assertiveBlocks.peek(),
                        EXPLICIT_CALL_APPLICATION,
                        tr.mathPExps.get(ctx.progExp()));
        stats.put(ctx, s);
    }

    //if the immediate parent is a callStmtCtx then add an actual stmt for this guy,
    //otherwise,
    @Override public void exitSwapStmt(Resolve.SwapStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, assertiveBlocks.peek(),
                        SWAP_APPLICATION, tr.mathPExps.get(ctx.left),
                        tr.mathPExps.get(ctx.right));
        stats.put(ctx, s);
    }

    @Override public void exitAssignStmt(Resolve.AssignStmtContext ctx) {
        VCRuleBackedStat s =
                new VCRuleBackedStat(ctx, assertiveBlocks.peek(),
                        FUNCTION_ASSIGN_APPLICATION,
                        tr.mathPExps.get(ctx.left),
                        tr.mathPExps.get(ctx.right));
        stats.put(ctx, s);
    }

    public PExp betaReduce(PExp start, PExp correspondence) {
        BasicBetaReducingListener v =
                new BasicBetaReducingListener(correspondence, start);
        start.accept(v);
        return v.getBetaReducedExp();
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
            if (declaredType instanceof PTFamily ) {
                PExp constraint = ((PTFamily) declaredType).getConstraint();

                constraint = constraint.substitute(
                        getSpecializationsForFacility(p.getTypeQualifier()));
                resultingAssumptions.add(constraint.substitute(
                        declaredType.getExemplarAsPSymbol(), p.asPSymbol())); // ASSUME TC (type constraint -- if we're conceptual)
            }
            else if (declaredType instanceof PTRepresentation)  {
                ProgReprTypeSymbol repr =
                        ((PTRepresentation) declaredType).getReprTypeSymbol();
                PExp convention = repr.getConvention();

                resultingAssumptions.add(convention.substitute(
                        declaredType.getExemplarAsPSymbol(), p.asPSymbol())); // ASSUME RC (repr convention -- if we're conceptual)
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
            if (p.getMode() == ProgParameterSymbol.ParameterMode.PRESERVES
                    || p.getMode() == ProgParameterSymbol.ParameterMode.RESTORES) {
                PExp equalsExp =
                        new PSymbol.PSymbolBuilder("=")
                                .arguments(paramExp, incParamExp)
                                .style(PSymbol.DisplayStyle.INFIX)
                                .mathType(g.BOOLEAN).build();
                result.add(equalsExp);
            }
            else if (p.getMode() == ProgParameterSymbol.ParameterMode.CLEARS) {
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
    //PTRepresentation). This method won't do anything otherwise.*/
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
                //resultingClause =
                //        betaReduce(resultingClause,
                //                corrFnExp);
            }
        }
        return resultingClause;
    }
}