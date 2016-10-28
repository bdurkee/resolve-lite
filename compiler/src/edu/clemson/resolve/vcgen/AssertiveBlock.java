package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.Model.OutputModelObject;
import edu.clemson.resolve.codegen.ModelElement;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import edu.clemson.resolve.vcgen.stats.VCRuleBackedStat;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AssertiveBlock {

    private final ParserRuleContext definingTree;
    private final String blockDescription;
    private final VCConfirm finalConfirm;

    private final List<RuleApplicationStep> applicationSteps = new ArrayList<>();
    private final List<String> stats = new ArrayList<>();
    //TODO: If we wanted these to be actual VCRuleBackedStats we would need an immutable version of general
    // statements that takes a final, immutable copy of this
    //private final List<VCRuleBackedStat> stats = new ArrayList<>();

    public AssertiveBlock(@Nullable ParserRuleContext definingTree,
                          @NotNull VCConfirm finalConfirm,
                          @NotNull List<RuleApplicationStep> applicationSteps,
                          @NotNull List<VCRuleBackedStat> stats,
                          @NotNull String blockDescription) {
        this.definingTree = definingTree;
        this.finalConfirm = finalConfirm;
        this.blockDescription = blockDescription;

        this.applicationSteps.addAll(applicationSteps);
        for (VCRuleBackedStat s : stats) {
            this.stats.add(s.toString());
        }
        //this.stats.addAll(stats);
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

    @NotNull
    public VCConfirm getFinalConfirm() {
        return finalConfirm;
    }

    public List<RuleApplicationStep> getApplicationSteps() {
        return applicationSteps;
    }

    //Assertive block is a bunch of statements with a final confirm...
    @Override
    public String toString() {
        String result = "";
        for (String s : stats) {
            //for (VCRuleBackedState s : stats) {
            result += s + "\n";
        }
        result += finalConfirm.toString();
        return result;
    }
}
