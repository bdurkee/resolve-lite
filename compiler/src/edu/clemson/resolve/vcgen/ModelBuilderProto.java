package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSegments;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.typereasoning.TypeGraph;
import edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy;
import edu.clemson.resolve.vcgen.application.FunctionAssignApplicationStrategy;
import edu.clemson.resolve.vcgen.application.StatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.application.SwapApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.rsrg.semantics.*;
import org.rsrg.semantics.query.NameQuery;
import org.rsrg.semantics.query.UnqualifiedNameQuery;
import org.rsrg.semantics.symbol.*;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
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

    @Override public void enterModule(@NotNull Resolve.ModuleContext ctx) {
        moduleScope = symtab.moduleScopes.get(Utils.getModuleName(ctx));
    }

    @Override public void enterTypeRepresentationDecl(
            @NotNull Resolve.TypeRepresentationDeclContext ctx) {
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
            @NotNull Resolve.TypeRepresentationDeclContext ctx) {
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
        outputFile.chunks.add(block.build());
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
        for (PExp e : correspondence.splitIntoConjuncts()) {
            if ( !e.isEquality() ) {
                //Todo: This should be added to ErrorKind and checked somewhere better.
                throw new IllegalStateException(
                        "malformed correspondence, "
                                + "should be of the form "
                                + "conceptualvar1 = [exp_1]; ... conceptualvar_n = [exp_n]");
            }
            PSymbol eAsPSym = (PSymbol) e;
            PSegments elhs = (PSegments) eAsPSym.getArguments().get(0);
            PSymbol erhs = (PSymbol) eAsPSym.getArguments().get(1);
            start = start.substitute(elhs, erhs);
        }
        return start;
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