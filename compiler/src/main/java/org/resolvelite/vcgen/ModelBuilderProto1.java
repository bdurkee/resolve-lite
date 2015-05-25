package org.resolvelite.vcgen;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.*;
import org.resolvelite.semantics.programtype.PTNamed;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.vcgen.model.*;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.model2.ACTypeInit;

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
    private final VCOutputFile outputCollector = new VCOutputFile();
    private ModuleScopeBuilder moduleScope = null;

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
        return outputCollector;
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {

        ProgReprTypeSymbol s = symtab.ctxToSyms.get(ctx).toProgReprTypeSymbol();
        List<PExp> assumptions = getGlobalAssertionsOfType(requires());
        PExp convention = tr.getPExpFor(g, ctx.conventionClause());
        PExp typeInitEnsures =
                s.getRepresentationType().getInitializationEnsures();
        int i = 0;
        i = 0;
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
