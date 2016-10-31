package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.semantics.*;
import edu.clemson.resolve.semantics.programtype.*;
import edu.clemson.resolve.semantics.query.NameQuery;
import edu.clemson.resolve.vcgen.application.*;
import edu.clemson.resolve.vcgen.stats.*;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.query.OperationQuery;
import edu.clemson.resolve.semantics.query.SymbolTypeQuery;
import edu.clemson.resolve.semantics.query.UnqualifiedNameQuery;
import edu.clemson.resolve.semantics.symbol.*;
import edu.clemson.resolve.semantics.symbol.GlobalMathAssertionSymbol.ClauseType;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol.ParameterMode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy.ExplicitCallRuleApplyingListener;

//TODO: CAN CHANGE TO VISITOR I THINK.
public class VCGenerator extends ResolveBaseListener {

    private final AnnotatedModule tr;
    private final MathSymbolTable symtab;
    private final DumbMathClssftnHandler g;

    //TODO: in applyCallRule() in ModelBuilderProto, we should be going through
    //one of these static fields to apply the rule, we should make a class that
    //extends VCRuleBackedStat called VCCall which simply wraps a PExp representing
    //a call.
    public static final VCStatRuleApplicationStrategy<VCCall> EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    public static final VCStatRuleApplicationStrategy<VCCall> GENERAL_CALL_APPLICATION =
            new GeneralCallApplicationStrategy();
    //TODO:
    //Also have VCFuncAssign extends VCruleBackedStat, then you can have fields
    //which do things like getLhs(), getCall(), etc. That'd be nicer than doing
    //stats.getComponents().get(0), etc.
    private static final VCStatRuleApplicationStrategy<VCAssign> FUNCTION_ASSIGN_APPLICATION = new FunctionAssignApplicationStrategy();
    private static final VCStatRuleApplicationStrategy<VCSwap> SWAP_APPLICATION = new SwapApplicationStrategy();
    public static final ConditionalApplicationStrategy IF_APPLICATION = new ConditionalApplicationStrategy.IfApplicationStrategy();
    public static final ConditionalApplicationStrategy ELSE_APPLICATION = new ConditionalApplicationStrategy.ElseApplicationStrategy();

    /** A map from facility name to another map from formal parameter names to their actual substitutions. */
    private final Map<String, Map<PExp, PExp>> facilitySpecFormalActualMappings = new HashMap<>();
    private final ParseTreeProperty<VCRuleBackedStat> stats = new ParseTreeProperty<>();
    private final VCOutputFile outputFile;
    private ModuleScopeBuilder moduleScope = null;

    private ProgReprTypeSymbol currentTypeReprSym = null;

    private final Deque<VCAssertiveBlockBuilder> assertiveBlocks = new LinkedList<>();

    private OperationSymbol currentProcOpSym = null;
    private boolean withinCallStmt = false;
    private final RESOLVECompiler compiler;

    public VCGenerator(RESOLVECompiler compiler, AnnotatedModule module) {
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
        block.assume(g.getTrueExp());

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
                PExp RPC = implReq.get().substitute(implFormalsToActuals);

                //(RPC[rn ~> rn_exp, RR ~> IRR] /\ SpecRequires)
                result = g.formConjunct(RPC, result);
            }
        }
        //(RPC[rn ~> rn_exp, RR ~> IRR] /\ SpecRequires)[n ~> n_exp, r ~> IR]
        result = result.substitute(specFormalsToActuals);
        if (!result.isObviouslyTrue()) {
            block.finalConfirm(result);
            outputFile.addAssertiveBlocks(block.build());
        }
    }


    private List<PExp> reduceArgs(VCAssertiveBlockBuilder b, List<PExp> args) {
        List<PExp> result = new ArrayList<>();
        for (PExp progArg : args) {
            if (progArg instanceof PApply) { //i.e., we're dealing with a function application
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

    private PExp applyCallRuleToExp(ParserRuleContext ctx, VCAssertiveBlockBuilder block, PApply exp) {
        ExplicitCallRuleApplyingListener applier = new ExplicitCallRuleApplyingListener(ctx, block);
        exp.accept(applier);
        PExp finalConfirm = block.finalConfirm.getConfirmExp();
       // block.finalConfirm(finalConfirm, );
       /* if (applier.returnEnsuresArgSubstitutions.isEmpty()) {
            throw new IllegalStateException("something's screwy: " +
                    "shouldn't of tried applying " +
                    "call rule to: " + exp.printNested()+".. " +
                    "could happen too right now if there's no spec for the op");
        }*/
        return applier.returnEnsuresArgSubstitutions.get(exp);
    }

    @Override
    public void enterTypeRepresentationDecl(ResolveParser.TypeRepresentationDeclContext ctx) {
        Scope s = symtab.getScope(ctx);
        currentTypeReprSym = null;
        try {
            currentTypeReprSym = moduleScope.queryForOne(
                    new UnqualifiedNameQuery(ctx.name.getText())).toProgReprTypeSymbol();
        } catch (SymbolTableException e) {
        }
        List<PExp> opParamAntecedents =
                getAssertionsFromModuleFormalParameters(getAllModuleParameterSyms(),
                        this::extractAssumptionsFromParameter);

        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, s,
                        "Well_Def_Corr_Hyp=" + ctx.name.getText(), ctx)
                        .assume(opParamAntecedents)
                        .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        .assume(currentTypeReprSym.getConvention());
        assertiveBlocks.push(block);
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
    public void exitTypeRepresentationDecl(ResolveParser.TypeRepresentationDeclContext ctx) {
        PExp constraint = g.getTrueExp();
        PExp correspondence = g.getTrueExp();
        if (currentTypeReprSym == null) return;
        correspondence = currentTypeReprSym.getCorrespondence();

        if (currentTypeReprSym.getDefinition() != null) {
            constraint = currentTypeReprSym.getDefinition().getProgramType().getConstraint();
        }
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        PExp newConstraint = constraint.substitute(
                currentTypeReprSym.exemplarAsPSymbol(), currentTypeReprSym.conceptualExemplarAsPSymbol());
        newConstraint = newConstraint.withVCInfo(ctx.getStart(), "Constraint for type: " + ctx.name.getText());
        block.assume(correspondence.splitIntoConjuncts());
        //throw new UnsupportedOperationException("re-institute the final confirm for this dan");
        block.finalConfirm(newConstraint);
        outputFile.addAssertiveBlocks(block.build());
    }

    @Override
    public void enterTypeImplInit(ResolveParser.TypeImplInitContext ctx) {
        Scope s = symtab.getScope(ctx.getParent());
        PExp convention = currentTypeReprSym.getConvention();
        PExp correspondence = currentTypeReprSym.getCorrespondence();
        PExp typeInitEnsures = g.getTrueExp();
        List<ModuleParameterSymbol> moduleParamSyms = getAllModuleParameterSyms();

        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, s,
                        "T_Init_Hypo=" + currentTypeReprSym.getName(), ctx)
                        .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        .assume(getAssertionsFromModuleFormalParameters(moduleParamSyms,
                                this::extractAssumptionsFromParameter));
        assertiveBlocks.push(block);
    }

    @Override
    public void exitTypeImplInit(ResolveParser.TypeImplInitContext ctx) {
        PExp typeInitEnsures = g.getTrueExp();
        PExp convention = currentTypeReprSym.getConvention();
        PExp correspondence = currentTypeReprSym.getCorrespondence();
        if (currentTypeReprSym.getDefinition() != null) {
            typeInitEnsures = currentTypeReprSym.getDefinition().getProgramType().getInitializationEnsures();
        }
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        PExp newInitEnsures = typeInitEnsures.substitute(
                currentTypeReprSym.exemplarAsPSymbol(), currentTypeReprSym.conceptualExemplarAsPSymbol());
        //block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats));
        //block.confirm(convention);  //order here is important
        block.assume(correspondence);
        throw new UnsupportedOperationException("re-institute the final confirm for this dan");
       // block.finalConfirm(newInitEnsures, "Initialization-ensures clause of " + currentTypeReprSym.getName());
        //outputFile.addAssertiveBlock(block.build());
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
                        .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                        .assume(getModuleLevelAssertionsOfType(ClauseType.CONSTRAINT))
                        .assume(corrFnExpRequires)
                        .remember();

        //positive stats
        StmtListener l = new StmtListener(block, tr.exprASTs);
        ParseTreeWalker.DEFAULT.walk(l, ctx);   //walk all stmts in this context, processing all satisfied if branches
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
        try {
            List<ProgParameterSymbol> paramSyms = s.getSymbolsOfType(ProgParameterSymbol.class);

            currentProcOpSym = s.queryForOne(new OperationQuery(null, ctx.name,
                    Utils.apply(paramSyms, ProgParameterSymbol::getDeclaredType)));

            //This is the requires for the operation with some substutions made (see corrFnExp rule in HH-diss)
            PExp corrFnExpRequires = perParameterCorrFnExpSubstitute(paramSyms, currentProcOpSym.getRequires());
            List<PExp> opParamAntecedents = new ArrayList<>();
            Utils.apply(paramSyms, opParamAntecedents, this::extractAssumptionsFromParameter);
            Set<PExp> l = getModuleLevelAssertionsOfType(ClauseType.REQUIRES);
            VCAssertiveBlockBuilder block =
                    new VCAssertiveBlockBuilder(g, s,
                            "Correct_Op_Hypo=" + ctx.name.getText(), ctx)
                            .facilitySpecializations(facilitySpecFormalActualMappings)
                            .assume(getModuleLevelAssertionsOfType(ClauseType.REQUIRES))
                            //TODO: constraints should be added on demand via NOTICE:...
                            //.assume(getModuleLevelAssertionsOfType(ClauseType.CONSTRAINT))
                            .assume(opParamAntecedents) //we assume correspondence for reprs here automatically
                            .assume(corrFnExpRequires)
                            .remember();
            assertiveBlocks.push(block);
        } catch (SymbolTableException e) {
            throw new RuntimeException(e);   //this shouldn't happen now
        }
    }

    @Override
    public void exitProcedureDecl(ResolveParser.ProcedureDeclContext ctx) {
        Scope scope = symtab.getScope(ctx);
        List<ProgParameterSymbol> paramSyms = scope.getSymbolsOfType(ProgParameterSymbol.class);
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
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
        PExp corrFnExpEnsures = perParameterCorrFnExpSubstitute(paramSyms, currentProcOpSym.getEnsures())
                .withVCInfo(ctx.getStart(), "Ensures clause of " + ctx.name.getText());
        //postcondition[params 1..i <-- corr_fn_exp]
       List<PExp> paramConsequents = new ArrayList<>();
       Utils.apply(formalParameters, paramConsequents, this::extractConsequentsFromParameter);

        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
                .assume(corrFnExps)
                .confirm(ctx, g.formConjuncts(paramConsequents))
                .finalConfirm(corrFnExpEnsures);

        //outputFile.addAssertiveBlock(block.build());
        currentProcOpSym = null;
    }

    /*@Override public void exitVariableDeclGroup(
            ResolveParser.VariableDeclGroupContext ctx) {
        PTType type = tr.progTypeValues.get(ctx.type());
        MTType mathClssfctn = tr.mathTypeValues.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {

            if (type instanceof PTNamed) {
                PExp init = ((PTNamed)type).getInitializationEnsures();
                PSymbol v = new PSymbol.PSymbolBuilder(t.getText())
                        .mathClssfctn(mathClssfctn).progType(type).build();
                init = init.substitute(((PTNamed) type)
                        .getExemplarAsPSymbol(), v);
                assertiveBlocks.peek().assume(init);
            }
            else { //generic case
                assertiveBlocks.peek().assume(
                        g.formInitializationPredicate(type, t.getText()));
            }
        }
    }*/

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
                .map(assertion -> substituteByFacilities(facilities, assertion))
                .collect(Collectors.toSet());
    }

    private PExp substituteByFacilities(List<FacilitySymbol> facilities,
                                        GlobalMathAssertionSymbol e) {
        for (FacilitySymbol facility : facilities) {
            if (facility.getFacility().getSpecification().getModuleIdentifier().equals(e.getModuleIdentifier())) {
                return e.getEnclosedExp().substitute(getSpecializationsForFacility(facility.getName()));
            }
        }
        return e.getEnclosedExp();
    }

    private Map<PExp, PExp> getSpecializationsForFacility(@Nullable String facility) {
        Map<PExp, PExp> result = facilitySpecFormalActualMappings.get(facility);
        if (result == null) result = new HashMap<>();
        //TODO: If we come back null, go ahead query and specialize the specs...
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
        List<VCAssertiveBlockBuilder> branches = new ArrayList<>();

        public StmtListener(VCAssertiveBlockBuilder activeBuilder,
                            ParseTreeProperty<PExp> asts) {
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
            VCIfElse s = new VCIfElse(ctx, builder, IF_APPLICATION, thenStmts, elseStmts, progCondition);
            branches.add(builder);
            branches.add(new VCAssertiveBlockBuilder(builder));
            stats.put(ctx, s);
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
}