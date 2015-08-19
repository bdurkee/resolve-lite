package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
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
import org.rsrg.semantics.query.OperationQuery;
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

    public static final StatRuleApplicationStrategy EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    private final static StatRuleApplicationStrategy FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();
    private final static StatRuleApplicationStrategy SWAP_APPLICATION =
            new SwapApplicationStrategy();

    private final ParseTreeProperty<VCRuleBackedStat> stats =
            new ParseTreeProperty<>();
    private final VCOutputFile outputFile = new VCOutputFile();
    private ModuleScopeBuilder moduleScope = null;

    private ProgReprTypeSymbol currentTypeReprSym = null;

    private final Deque<VCAssertiveBlockBuilder> assertiveBlocks =
            new LinkedList<>();
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
        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, symtab.scopes.get(ctx),
                        "Well_Def_Corr_Hyp=" + ctx.name.getText(), ctx, tr)
                        .freeVars(getFreeVars(symtab.scopes.get(ctx)))
                        .assume(getModuleLevelAssertionsOfType(requires()))
                        .assume(currentTypeReprSym.getConvention());
        assertiveBlocks.push(block);
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
        PExp newConstraint =
                constraint.substitute(currentTypeReprSym.exemplarAsPSymbol(),
                        currentTypeReprSym.conceptualExemplarAsPSymbol());
        newConstraint =
                withCorrespondencePartsSubstituted(newConstraint,
                        correspondence);
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        block.finalConfirm(newConstraint);
        outputFile.addAssertiveBlock(block.build());
    }

    @Override public void enterTypeImplInit(Resolve.TypeImplInitContext ctx) {
        PExp convention = currentTypeReprSym.getConvention();
        PExp correspondence = currentTypeReprSym.getCorrespondence();
        PExp typeInitEnsures = g.getTrueExp();
        VCAssertiveBlockBuilder block =
                new VCAssertiveBlockBuilder(g, symtab.scopes.get(ctx),
                        "T_Init_Hypo=" + currentTypeReprSym.getName(), ctx, tr)
                        .assume(getModuleLevelAssertionsOfType(requires()));
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
        PExp newInitEnsures =
                typeInitEnsures.substitute(currentTypeReprSym.exemplarAsPSymbol(),
                        currentTypeReprSym.conceptualExemplarAsPSymbol());
        newInitEnsures =
                withCorrespondencePartsSubstituted(newInitEnsures,
                        correspondence);
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats));
        block.confirm(convention).finalConfirm(newInitEnsures);
        outputFile.addAssertiveBlock(block.build());
    }

    @Override public void enterProcedureDecl(Resolve.ProcedureDeclContext ctx) {
        Scope s = symtab.scopes.get(ctx);
        try {
            List<ProgParameterSymbol> paramSyms =
                    s.getSymbolsOfType(ProgParameterSymbol.class);
            OperationSymbol op = s.queryForOne(
                    new OperationQuery(null, ctx.name,
                            paramSyms.stream()
                                    .map(ProgParameterSymbol::getDeclaredType)
                                    .collect(Collectors.toList())));

            PExp corrFnExpRequires = substituteCorrFnExpIntoClause(paramSyms,
                    ctx, op.getRequires()); //precondition[params 1..i <-- corr_fn_exp]
            PExp corrFnExpEnsures = substituteCorrFnExpIntoClause(paramSyms,
                    ctx, op.getEnsures()); //postcondition[params 1..i <-- corr_fn_exp]

            VCAssertiveBlockBuilder block =
                    new VCAssertiveBlockBuilder(g, s,
                            "Correct_Op_Hypo="+ctx.name.getText(), ctx, tr)
                            .freeVars(getFreeVars(s))
                            .assume(getAllParameterAssumptions(paramSyms))
                            .assume(getModuleLevelAssertionsOfType(requires()))
                            .assume(getModuleLevelAssertionsOfType(constraint()))
                            .assume(corrFnExpRequires)
                            .remember()
                            .confirm(getAllParameterConfirms(paramSyms))
                            .finalConfirm(corrFnExpEnsures);
            assertiveBlocks.push(block);
        }
        catch (DuplicateSymbolException|NoSuchSymbolException e) {
            e.printStackTrace();    //shouldn't happen, we wouldn't be in vcgen if it did
        }
    }


    @Override public void exitProcedureDecl(Resolve.ProcedureDeclContext ctx) {
        VCAssertiveBlockBuilder block = assertiveBlocks.pop();
        block.stats(Utils.collect(VCRuleBackedStat.class, ctx.stmt(), stats));
        outputFile.addAssertiveBlock(block.build());
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

    public PExp withCorrespondencePartsSubstituted(PExp start,
                                                   PExp correspondence) {
        CorrespondenceReducingVisitor v =
                new CorrespondenceReducingVisitor(correspondence, start);
        start.accept(v);
        return v.getReducedExp();
    }

    private List<PExp> getAllParameterAssumptions(
            List<ProgParameterSymbol> parameters) {
        List<PExp> resultingAssumptions = new ArrayList<>();
        for (ProgParameterSymbol p : parameters) {
            PExp paramExp = p.asPSymbol();
            if ( p.getDeclaredType() instanceof PTNamed) {
                //both PTFamily AND PTRepresentation are a PTNamed
                PTNamed declaredType = (PTNamed)p.getDeclaredType();
                PExp exemplar =
                        new PSymbol.PSymbolBuilder(
                                declaredType.getExemplarName())
                                .mathType(declaredType.toMath()).build();
                PExp init = ((PTNamed) declaredType).getInitializationEnsures();
                if (!init.equals(g.getTrueExp())) {
                    resultingAssumptions.add(init.substitute(exemplar, paramExp));  // ASSUME IC (initialization constraint -- not in correct_op_hypo -- BUT in proc_decl_rule!)
                }
                if (declaredType instanceof PTFamily ) {
                    PExp constraint = ((PTFamily) declaredType).getConstraint();
                    resultingAssumptions.add(constraint.substitute(
                            declaredType.getExemplarAsPSymbol(), paramExp)); // ASSUME TC (type constraint -- if we're conceptual)
                }
                else  {
                    ProgReprTypeSymbol repr =
                            ((PTRepresentation) declaredType).getReprTypeSymbol();
                    PExp convention = repr.getConvention();

                    resultingAssumptions.add(convention.substitute(
                            declaredType.getExemplarAsPSymbol(), paramExp)); // ASSUME RC (repr convention -- if we're conceptual)
                    resultingAssumptions.add(repr.getCorrespondence()); // ASSUME Corr_Fn_Exp (the correspondence function/relation untouched)
                }
            }
        }
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

    //The only way I'm current aware of a local requires clause getting changed
    //is by passing a locally defined type  to an operation (something of type
    //PTRepresentation). This method won't do anything otherwise.
    private PExp substituteCorrFnExpIntoClause(List<ProgParameterSymbol> params,
                                               ParserRuleContext functionCtx,
                                               ParserRuleContext reqOrEns) {
        List<PExp> result = new ArrayList<>();
        PExp resultingClause = tr.getPExpFor(g, reqOrEns);
        for (ProgParameterSymbol p : params) {
            if (p.getDeclaredType() instanceof PTRepresentation) {
                ProgReprTypeSymbol repr =
                        ((PTRepresentation) p.getDeclaredType()).getReprTypeSymbol();

                PExp corrFnExp = repr.getCorrespondence();
                resultingClause =
                        resultingClause.substitute(repr.exemplarAsPSymbol(),
                                repr.conceptualExemplarAsPSymbol());

                resultingClause =
                        withCorrespondencePartsSubstituted(resultingClause,
                                corrFnExp);
                result.add(resultingClause);
            }
        }
        return g.formConjuncts(result);
    }
}