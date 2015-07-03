package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public abstract class AssertiveBlock extends OutputModelObject {

    private final Set<PSymbol> freeVars = new LinkedHashSet<>();
    private final AnnotatedTree annotations;
    private final ParserRuleContext definingTree;
    private final TypeGraph g;
    private final String blockDescription;

    @ModelElement public final VCRuleBackedStat finalConfirm;
    @ModelElement public final List<VCRuleBackedStat> stats = new ArrayList<>();
    @ModelElement public final List<RuleApplicationStep> applicationSteps =
            new ArrayList<>();

    public AssertiveBlock(TypeGraph g, ParserRuleContext definingTree,
                          VCConfirm finalConfirm, AnnotatedTree annotations,
                          List<VCRuleBackedStat> stats, Collection<PSymbol> freeVars,
                          List<RuleApplicationStep> applicationSteps, String blockDescription) {
        this.g = g;
        this.definingTree = definingTree;
        this.annotations = annotations;
        this.finalConfirm = finalConfirm;
        this.stats.addAll(stats);
        this.freeVars.addAll(freeVars);
        this.applicationSteps.addAll(applicationSteps);
        this.blockDescription = blockDescription;
    }

    public String getDescription() {
        return blockDescription;
    }

    public TypeGraph getTypeGraph() {
        return g;
    }

    public AnnotatedTree getAnnotations() {
        return annotations;
    }

    public ParserRuleContext getDefiningTree() {
        return definingTree;
    }

    public String getText() {
        return Utils.getRawText(definingTree);
    }

    public VCRuleBackedStat getFinalConfirm() {
        return finalConfirm;
    }

    public Set<PSymbol> getFreeVars() {
        return freeVars;
    }

    public List<? extends VCRuleBackedStat> getStats() {
        return stats;
    }

    public List<RuleApplicationStep> getApplicationSteps() {
        return applicationSteps;
    }
}
