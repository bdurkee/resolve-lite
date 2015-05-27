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
import org.resolvelite.vcgen.applicationstrategies.ExplicitCallApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.FunctionAssignApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;
import org.resolvelite.vcgen.model.*;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds assertive code and applies proof rules to the code within.
 */
public class ModelBuilderProto1 extends ResolveBaseListener {

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

    public static final RuleApplicationStrategy EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    private final static RuleApplicationStrategy FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();

    public ModelBuilderProto1(VCGenerator gen, SymbolTable symtab) {
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

    @Override public void enterTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        curTypeRepr = ctx;
    }

    @Override public void enterConstraintClause(
            @NotNull ResolveParser.ConstraintClauseContext ctx) {
        if (ctx.getParent() instanceof
                ResolveParser.TypeRepresentationDeclContext) {
            curAssertiveBuilder = new VCAssertiveBlockBuilder(g,
                    symtab.scopes.get(curTypeRepr),
                    "type constraint hypothesis: " + curTypeRepr.name.getText(),
                    ctx, tr).assume(getGlobalAssertionsOfType(requires()));
        }
    }

    @Override public void exitConstraintClause(
            @NotNull ResolveParser.ConstraintClauseContext ctx) {
        if (ctx.getParent() instanceof
                ResolveParser.TypeRepresentationDeclContext) {
        }
    }



    @Override public void enterTypeImplInit(
            @NotNull ResolveParser.TypeImplInitContext ctx) {
        curAssertiveBuilder = new VCAssertiveBlockBuilder(g,
                symtab.scopes.get(ctx),
                "type initialization hypothesis: " + curTypeRepr.name.getText(),
                ctx, tr).assume(getGlobalAssertionsOfType(requires()));
    }

    @Override public void exitTypeImplInit(
            @NotNull ResolveParser.TypeImplInitContext ctx) {
        ProgReprTypeSymbol s = symtab.ctxToSyms.get(ctx).toProgReprTypeSymbol();

        PExp convention = tr.getPExpFor(g, curTypeRepr.conventionClause());
        PExp correspondence = s.getCorrespondence();
        PExp typeInitEnsures = g.getTrueExp();

        curAssertiveBuilder.stats(Utils.collect(VCRuleBackedStat.class,
                ctx.stmt(), stats));

        if ( s.getDefinition() != null ) {
            typeInitEnsures =
                    s.getDefinition().getProgramType()
                            .getInitializationEnsures();
        }
        PExp newInitEnsures = typeInitEnsures.substitute(s.exemplarAsPSymbol(),
                s.getConceptualExemplarAsPDot());

        for (PExp e : correspondence.splitIntoConjuncts()) {
            if (!e.isEquality()) {
                //Todo: This should be added to ErrorKind and checked somewhere better.
                throw new IllegalStateException("malformed correspondence, " +
                        "should be of the form " +
                        "conceptualvar1 = [exp_1]; ... conceptualvar_n = [exp_n]");
            }
            PSymbol eAsPSym = (PSymbol) e;
            PDot elhs =  (PDot) eAsPSym.getArguments().get(0);
            PSymbol erhs =  (PSymbol) eAsPSym.getArguments().get(1);
            newInitEnsures = newInitEnsures.substitute(elhs, erhs);
        }
        curAssertiveBuilder.confirm(convention).finalConfirm(newInitEnsures);
        outputFile.chunks.add(curAssertiveBuilder.build());
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        curTypeRepr = null;

    }

    @Override public void exitStmt(@NotNull ResolveParser.StmtContext ctx) {
        stats.put(ctx, stats.get(ctx.getChild(0)));
    }

    @Override public void exitAssignStmt(@NotNull ResolveParser.AssignStmtContext ctx) {
        stats.put(ctx, new VCRuleBackedStat(ctx, curAssertiveBuilder,
                FUNCTION_ASSIGN_APPLICATION, tr.mathPExps.get(ctx.left),
                tr.mathPExps.get(ctx.right)));
    }

    public static Predicate<Symbol> constraint() {
        return s -> s.getDefiningTree() instanceof  //
                ResolveParser.ConstraintClauseContext;
    }

    public static Predicate<Symbol> requires() {
        return s -> s.getDefiningTree() instanceof //
                ResolveParser.RequiresClauseContext;
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
