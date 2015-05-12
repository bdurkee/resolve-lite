package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public abstract class AssertiveCode extends OutputModelObject {

    private final Set<PSymbol> freeVars = new LinkedHashSet<>();
    private final AnnotatedTree annotations;
    private final ParserRuleContext definingTree;
    private final TypeGraph g;

    @ModelElement public final VCConfirm finalConfirm;
    @ModelElement public final List<VCRuleBackedStat> stats = new ArrayList<>();
    @ModelElement public final List<RuleApplicationStep> applicationSteps =
            new ArrayList<>();

    public AssertiveCode(TypeGraph g, ParserRuleContext definingTree,
            VCConfirm finalConfirm, AnnotatedTree annotations,
            List<VCRuleBackedStat> stats, Collection<PSymbol> freeVars,
            List<RuleApplicationStep> applicationSteps) {
        this.g = g;
        this.definingTree = definingTree;
        this.annotations = annotations;
        this.finalConfirm = finalConfirm;
        this.stats.addAll(stats);
        this.freeVars.addAll(freeVars);
        this.applicationSteps.addAll(applicationSteps);
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

    public VCConfirm getFinalConfirm() {
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
