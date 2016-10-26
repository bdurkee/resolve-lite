package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.Model;
import edu.clemson.resolve.codegen.Model.OutputModelObject;
import edu.clemson.resolve.codegen.ModelElement;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AssertiveBlock extends OutputModelObject {

    private final ParserRuleContext definingTree;
    private final String blockDescription;

    @ModelElement
    public final VCConfirm finalConfirm;
    @ModelElement
    public final List<RuleApplicationStep> applicationSteps = new ArrayList<>();

    public AssertiveBlock(@Nullable ParserRuleContext definingTree,
                          @NotNull VCConfirm finalConfirm,
                          @NotNull List<RuleApplicationStep> applicationSteps,
                          @NotNull String blockDescription) {
        this.definingTree = definingTree;
        this.finalConfirm = finalConfirm;
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

    @NotNull
    public VCConfirm getFinalConfirm() {
        return finalConfirm;
    }

    public List<RuleApplicationStep> getApplicationSteps() {
        return applicationSteps;
    }
}
