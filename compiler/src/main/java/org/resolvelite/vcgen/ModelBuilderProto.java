package org.resolvelite.vcgen;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PDot;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.*;
import org.resolvelite.semantics.symbol.*;
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
    private PExp moduleLevelRequires, moduleLevelConstraint = null;
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

    @Override public void enterConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
        moduleScope = symtab.moduleScopes.get(ctx.name.getText());
    }

    public VCOutputFile getOutputFile() {
        return outputFile;
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
                        .assume(getGlobalAssertionsOfType(requires()));
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
                        s.getConceptualExemplarAsPDot());
        newInitEnsures =
                withCorrespondencePartsSubstituted(newInitEnsures,
                        correspondence);
        curAssertiveBuilder.stats(Utils.collect(VCRuleBackedStat.class,
                ctx.stmt(), stats));
        curAssertiveBuilder.confirm(convention).finalConfirm(newInitEnsures);
        outputFile.chunks.add(curAssertiveBuilder.build());
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        ProgReprTypeSymbol s = symtab.ctxToSyms.get(ctx).toProgReprTypeSymbol();
        curAssertiveBuilder =
                new VCAssertiveBlockBuilder(g, symtab.scopes.get(ctx),
                        "Well_Def_Corr_Hyp=" + ctx.name.getText(), ctx, tr)
                        .assume(getGlobalAssertionsOfType(requires())).assume(
                                s.getConvention());

        PExp constraint = g.getTrueExp();
        PExp correspondence = s.getCorrespondence();
        if ( s.getDefinition() != null ) {
            constraint = s.getDefinition().getProgramType().getConstraint();
        }
        PExp newConstraint =
                constraint.substitute(s.exemplarAsPSymbol(),
                        s.getConceptualExemplarAsPDot());
        newConstraint =
                withCorrespondencePartsSubstituted(newConstraint,
                        correspondence);
        curAssertiveBuilder.finalConfirm(newConstraint);
        outputFile.chunks.add(curAssertiveBuilder.build());
    }

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

    private List<PExp> getGlobalAssertionsOfType(
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
