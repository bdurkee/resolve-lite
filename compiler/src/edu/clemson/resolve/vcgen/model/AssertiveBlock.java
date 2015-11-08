package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public abstract class AssertiveBlock extends OutputModelObject {

    private final ParserRuleContext definingTree;
    private final String blockDescription;

    @ModelElement public final VCConfirm finalConfirm;
    @ModelElement public final List<VCRuleBackedStat> stats = new ArrayList<>();
    @ModelElement public final List<RuleApplicationStep> applicationSteps =
            new ArrayList<>();

    public AssertiveBlock(ParserRuleContext definingTree, VCConfirm finalConfirm,
                          List<VCRuleBackedStat> stats,
                          List<RuleApplicationStep> applicationSteps,
                          String blockDescription) {
        this.definingTree = definingTree;
        this.finalConfirm = finalConfirm;
        this.stats.addAll(stats);
        this.applicationSteps.addAll(applicationSteps);
        this.blockDescription = blockDescription;
    }

    public String getDescription() {
        return blockDescription;
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

    public List<? extends VCRuleBackedStat> getStats() {
        return stats;
    }

    public List<RuleApplicationStep> getApplicationSteps() {
        return applicationSteps;
    }
}
