package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public abstract class AssertiveCode extends OutputModelObject {

    private final Set<PExp> freeVars = new LinkedHashSet<>();
    private final AnnotatedTree annotations;
    private final ParserRuleContext definingTree;
    private final TypeGraph g;

    @ModelElement private final VCConfirm finalConfirm;
    @ModelElement private final List<VCRuleBackedStat> stats =
            new ArrayList<>();
    @ModelElement private final List<AssertiveCode> applicationSteps =
            new ArrayList<>();

    public AssertiveCode(TypeGraph g, ParserRuleContext definingTree,
            VCConfirm finalConfirm, AnnotatedTree annotations,
            List<VCRuleBackedStat> stats, Collection<? extends PExp> freeVars,
            List<AssertiveCode> applicationSteps) {
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

    public ParserRuleContext getDefiningCtx() {
        return definingTree;
    }

    public VCConfirm getFinalConfirm() {
        return finalConfirm;
    }

    public List<? extends VCRuleBackedStat> getStats() {
        return stats;
    }

    public List<AssertiveCode> getApplicationSteps() {
        return applicationSteps;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("free vars: ");
        for (PExp var : freeVars) {
            sb.append(var + " : " + var.getMathType()).append(", ");
        }
        sb.append("\n");
        for (VCRuleBackedStat s : stats) {
            sb.append(s).append("\n");
        }
        return sb.append(finalConfirm).toString();
    }
}
