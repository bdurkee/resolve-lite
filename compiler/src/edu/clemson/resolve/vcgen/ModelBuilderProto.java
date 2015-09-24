package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.TypeGraph;
import edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy;
import edu.clemson.resolve.vcgen.application.FunctionAssignApplicationStrategy;
import edu.clemson.resolve.vcgen.application.StatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.application.SwapApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.PTFamily;
import org.rsrg.semantics.programtype.PTNamed;
import org.rsrg.semantics.programtype.PTRepresentation;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.NameQuery;
import org.rsrg.semantics.query.OperationQuery;
import org.rsrg.semantics.query.SymbolTypeQuery;
import org.rsrg.semantics.query.UnqualifiedNameQuery;
import org.rsrg.semantics.symbol.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ModelBuilderProto extends ResolveBaseListener {
    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final RESOLVECompiler compiler;
    private final TypeGraph g;

    public static final StatRuleApplicationStrategy<VCRuleBackedStat> EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    private final static StatRuleApplicationStrategy<VCRuleBackedStat> FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();
    private final static StatRuleApplicationStrategy<VCRuleBackedStat> SWAP_APPLICATION =
            new SwapApplicationStrategy();

    private final ParseTreeProperty<VCRuleBackedStat> stats =
            new ParseTreeProperty<>();
    private final VCOutputFile outputFile = new VCOutputFile();
    private ModuleScopeBuilder moduleScope = null;

    private ProgReprTypeSymbol currentTypeReprSym = null;

    private final Deque<VCAssertiveBlockBuilder> assertiveBlocks =
            new LinkedList<>();

    private OperationSymbol currentProcOpSym = null;

    public ModelBuilderProto(VCGenerator gen, SymbolTable symtab) {
        this.symtab = symtab;
        this.tr = gen.getModule();
        this.compiler = gen.getCompiler();
        this.g = symtab.getTypeGraph();
    }

    public VCOutputFile getOutputFile() {
        return outputFile;
    }

    @Override public void enterModule(Resolve.ModuleContext ctx) {
        moduleScope = symtab.moduleScopes.get(Utils.getModuleName(ctx));
    }

    @Override public void enterTypeRepresentationDecl(
            Resolve.TypeRepresentationDeclContext ctx) {
        currentTypeReprSym = null;
        try {
            currentTypeReprSym =
                    moduleScope.queryForOne(new UnqualifiedNameQuery(
                            ctx.name.getText())).toProgReprTypeSymbol();
        } catch (NoSuchSymbolException|DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    ctx.name.getText());
        }
        List<ProgParameterSymbol> moduleParamSyms = getAllModuleParameterSyms();
        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, symtab.scopes.get(ctx),
                        "Well_Def_Corr_Hyp=" + ctx.name.getText(), ctx, tr)
                        .freeVars(getFreeVars(symtab.scopes.get(ctx)))
                        .assume(getAllParameterAssumptions(moduleParamSyms))
                        .assume(getAllModuleLevelAssertionsOfType(requires()))
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
        PExp convention = currentTypeReprSym.getConvention();
        PExp correspondence = currentTypeReprSym.getCorrespondence();
        PExp typeInitEnsures = g.getTrueExp();
        List<ProgParameterSymbol> moduleParamSyms = getAllModuleParameterSyms();

        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, symtab.scopes.get(ctx),
                    "T_Init_Hypo=" + currentTypeReprSym.getName(), ctx, tr)
                    .assume(getAllModuleLevelAssertionsOfType(requires()))
                    .assume(getAllParameterAssumptions(moduleParamSyms));

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
                ctx, ctx.requiresClause()); //precondition[params 1..i <-- conc.X]

        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, s,
                        "Proc_Decl_rule="+ctx.name.getText(), ctx, tr)
                        .freeVars(getFreeVars(s))
                        .assume(getAllParameterAssumptions(paramSyms))
                        .assume(getAllModuleLevelAssertionsOfType(requires()))
                        .assume(getAllModuleLevelAssertionsOfType(constraint()))
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
                ctx, ctx.ensuresClause()); //postcondition[params 1..i <-- corr_fn_exp]
        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
                .confirm(getAllParameterConfirms(paramSyms))
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
                            "Correct_Op_Hypo="+ctx.name.getText(), ctx, tr)
                            .freeVars(getFreeVars(s))
                            .assume(getAllModuleLevelAssertionsOfType(requires()))
                            .assume(getAllModuleLevelAssertionsOfType(constraint()))
                            .assume(getAllParameterAssumptions(paramSyms)) //we assume correspondence for reprs here automatically
                            .assume(corrFnExpRequires)
                            .remember();
            assertiveBlocks.push(block);
        }
        catch (DuplicateSymbolException|NoSuchSymbolException e) {
            e.printStackTrace();    //shouldn't happen, we wouldn't be in vcgen if it did
        }
    }

    @Override public void exitProcedureDecl(Resolve.ProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        List<ProgParameterSymbol> paramSyms =
                s.getSymbolsOfType(ProgParameterSymbol.class);
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();

        List<PExp> corrFnExps = paramSyms.stream()
                .filter(p -> p.getDeclaredType() instanceof PTRepresentation)
                .map(p -> (PTRepresentation)p.getDeclaredType())
                .map(p -> p.getReprTypeSymbol().getCorrespondence())
                .collect(Collectors.toList());
        PExp corrFnExpEnsures = perParameterCorrFnExpSubstitute(paramSyms,
                ctx, currentProcOpSym.getEnsures()); //postcondition[params 1..i <-- corr_fn_exp]

        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats))
            .confirm(getAllParameterConfirms(paramSyms))
            .assume(corrFnExps)
                .finalConfirm(corrFnExpEnsures);

        outputFile.addAssertiveBlock(block.build());
        currentProcOpSym = null;
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
                        EXPLICIT_CALL_APPLICATION, tr.mathPExps.get(ctx
                        .progExp()));
        stats.put(ctx, s);
    }

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
                        tr.mathPExps.get(ctx.left), tr.mathPExps.get(ctx.right));
        stats.put(ctx, s);
    }

    public List<Symbol> getFreeVars(Scope s) {
        return s.getSymbolsOfType(Symbol.class).stream()
                .filter(x -> x instanceof ProgParameterSymbol ||
                        x instanceof ProgVariableSymbol)
                .collect(Collectors.toList());
    }

    public static Predicate<Symbol> constraint() {
        return s -> s.getDefiningTree() instanceof
                Resolve.ConstraintClauseContext;
    }

    public static Predicate<Symbol> requires() {
        return s -> s.getDefiningTree() instanceof
                Resolve.RequiresClauseContext;
    }

    public PExp betaReduce(PExp start, PExp correspondence) {
        BasicBetaReducingListener v =
                new BasicBetaReducingListener(correspondence, start);
        start.accept(v);
        return v.getBetaReducedExp();
    }

    private List<PExp> getAllParameterAssumptions(Scope opScope) {
        //you really need to query for the parameters so that any generic types
        //get properly substituted.
        List<ProgParameterSymbol> formalParameters = opScope.query(
                new SymbolTypeQuery<ProgParameterSymbol>(
                        ProgParameterSymbol.class));
        formalParameters = formalParameters.stream()
                .filter(p -> !p.isModuleParameter()).collect(Collectors.toList());

        for (ProgParameterSymbol p : formalParameters) {

        }
      /*  for (Resolve.ParameterDeclGroupContext grp : parameterGroupings) {
            PTType groupType = tr.progTypeValues.get(grp.type());
            for (TerminalNode t : grp.ID()) {
                PSymbol asPSym = new PSymbol.PSymbolBuilder(t.getText())
                        .progType(groupType).mathType(groupType.toMath())
                        .build();
                extractRelevantAssumptionsFromType();
            }
        }*/
    }

    private OperationSymbol getOperation(String name)
    private List<PExp> extractRelevantAssumptionsFromParameter(
            String typeQualifier, PTType type, PSymbol p) {
        List<PExp> resultingAssumptions = new ArrayList<>();
        if ( type instanceof PTNamed) {
            //both PTFamily AND PTRepresentation are a PTNamed
            PTNamed declaredType = (PTNamed)type;
            PExp exemplar = declaredType.getExemplarAsPSymbol();
            if (declaredType instanceof PTFamily ) {
                PExp constraint = ((PTFamily) declaredType).getConstraint();

                resultingAssumptions.add(constraint.substitute(
                        declaredType.getExemplarAsPSymbol(), p)); // ASSUME TC (type constraint -- if we're conceptual)
            }
            else if (declaredType instanceof PTRepresentation)  {
                ProgReprTypeSymbol repr =
                        ((PTRepresentation) declaredType).getReprTypeSymbol();
                PExp convention = repr.getConvention();

                resultingAssumptions.add(convention.substitute(
                        declaredType.getExemplarAsPSymbol(), p)); // ASSUME RC (repr convention -- if we're conceptual)
                resultingAssumptions.add(repr.getCorrespondence());
            }
        }
        else { //PTGeneric
            resultingAssumptions.add(
                    g.formInitializationPredicate(type, p.getName()));
        }
        return resultingAssumptions;
    }

    private PExp substituteFacilityArgs(String facility, PExp e) {
        FacilitySymbol facility = moduleScope.queryForOne(new NameQuery())
    }
     /*   List<PExp> resultingAssumptions = opScope

        for (ProgParameterSymbol p : parameters) {
            PExp paramExp = p.asPSymbol();
            if ( p.getDeclaredType() instanceof PTNamed) {
                //both PTFamily AND PTRepresentation are a PTNamed
                PTNamed declaredType = (PTNamed)p.getDeclaredType();
                PExp exemplar = declaredType.getExemplarAsPSymbol();
                if (declaredType instanceof PTFamily ) {
                    PExp constraint = ((PTFamily) declaredType).getConstraint();
                    resultingAssumptions.add(constraint.substitute(
                            declaredType.getExemplarAsPSymbol(), paramExp)); // ASSUME TC (type constraint -- if we're conceptual)
                }
                else if (declaredType instanceof PTRepresentation)  {
                    ProgReprTypeSymbol repr =
                            ((PTRepresentation) declaredType).getReprTypeSymbol();
                    PExp convention = repr.getConvention();

                    resultingAssumptions.add(convention.substitute(
                            declaredType.getExemplarAsPSymbol(), paramExp)); // ASSUME RC (repr convention -- if we're conceptual)
                    resultingAssumptions.add(repr.getCorrespondence());
                }
            }
            else { //PTGeneric
                resultingAssumptions.add(g.formInitializationPredicate(
                        p.getDeclaredType(), p.getName()));
            }
        }*/
        return resultingAssumptions;
    }

    private List<PExp> getAllParameterConfirms(
            List<ProgParameterSymbol> parameters) {
        List<PExp> resultingConfirms = new ArrayList<>();
        for (ProgParameterSymbol p : parameters) {
            PSymbol.PSymbolBuilder temp =
                    new PSymbol.PSymbolBuilder(p.getName()).mathType(p
                            .getDeclaredType().toMath());

            PExp incParamExp = temp.incoming(true).build();
            PExp paramExp = temp.incoming(false).build();
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
                    resultingConfirms.add(convention.substitute(
                            t.getExemplarAsPSymbol(), paramExp));
                }
                if (p.getMode() == ProgParameterSymbol.ParameterMode.PRESERVES
                        || p.getMode() == ProgParameterSymbol.ParameterMode.RESTORES) {
                    PExp equalsExp =
                            new PSymbol.PSymbolBuilder("=")
                                    .arguments(paramExp, incParamExp)
                                    .style(PSymbol.DisplayStyle.INFIX)
                                    .mathType(g.BOOLEAN).build();
                    resultingConfirms.add(equalsExp);
                }
                else if (p.getMode() == ProgParameterSymbol.ParameterMode.CLEARS) {
                    PExp init = ((PTNamed) p.getDeclaredType()) //
                            .getInitializationEnsures() //
                            .substitute(exemplar, paramExp);
                    resultingConfirms.add(init);
                }
            }
        }
        return resultingConfirms;
    }

    private List<PExp> getAllModuleLevelAssertionsOfType(
            Predicate<Symbol> assertionType) {
        List<GlobalMathAssertionSymbol> result = moduleScope.query(
                new SymbolTypeQuery<GlobalMathAssertionSymbol>
                        (GlobalMathAssertionSymbol.class,
                                SymbolTable.ImportStrategy.IMPORT_NAMED,
                                SymbolTable.FacilityStrategy.FACILITY_GENERIC));
        return result.stream()
                .filter(assertionType)
                .map(GlobalMathAssertionSymbol::getEnclosedExp)
                .collect(Collectors.toList());
    }

    //The only way I'm current aware of a local requires clause getting changed
    //is by passing a locally defined type  to an operation (something of type
    //PTRepresentation). This method won't do anything otherwise.
    private PExp perParameterCorrFnExpSubstitute(List<ProgParameterSymbol> params,
                                                 ParserRuleContext functionCtx,
                                                 ParserRuleContext reqOrEns) {
        List<PExp> result = new ArrayList<>();
        PExp resultingClause = tr.getPExpFor(g, reqOrEns);
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