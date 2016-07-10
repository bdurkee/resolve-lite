package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VCRuleBackedStat extends OutputModelObject {

    private final ParserRuleContext definingCtx;
    protected final List<PExp> statComponents = new ArrayList<>();
    private final VCStatRuleApplicationStrategy applicationStrategy;
    private final VCAssertiveBlockBuilder enclosingBlock;

    public VCRuleBackedStat(ParserRuleContext ctx,
                            VCAssertiveBlockBuilder block,
                            VCStatRuleApplicationStrategy apply,
                            PExp... e) {
        this.statComponents.addAll(Arrays.asList(e));
        this.applicationStrategy = apply;
        this.enclosingBlock = block;
        this.definingCtx = ctx;
    }

    public String getText() {
        if (definingCtx != null) return Utils.getRawText(definingCtx);
        return "";
    }

    @NotNull
    public List<PExp> getStatComponents() {
        return statComponents;
    }

    @SuppressWarnings("unchecked")
    public AssertiveBlock reduce() {
        return applicationStrategy.applyRule(enclosingBlock, this);
    }

    public String getApplicationDescription() {
        return applicationStrategy.getDescription();
    }

    public VCAssertiveBlockBuilder getEnclosingBlock() {
        return enclosingBlock;
    }

    public ParserRuleContext getDefiningContext() {
        return definingCtx;
    }
}