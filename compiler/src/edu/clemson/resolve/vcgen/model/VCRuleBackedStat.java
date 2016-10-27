package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.Model.OutputModelObject;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class VCRuleBackedStat extends OutputModelObject {

    final ParserRuleContext definingCtx;
    final VCStatRuleApplicationStrategy applicationStrategy;
    final VCAssertiveBlockBuilder enclosingBlock;

    public VCRuleBackedStat(ParserRuleContext ctx,
                            VCAssertiveBlockBuilder block,
                            VCStatRuleApplicationStrategy apply) {
        this.applicationStrategy = apply;
        this.enclosingBlock = block;
        this.definingCtx = ctx;
    }

    public String getText() {
        return Utils.getRawText(definingCtx);
    }

    @NotNull
    public abstract VCRuleBackedStat copyWithBlock(@NotNull VCAssertiveBlockBuilder b);

    @SuppressWarnings("unchecked")
    public AssertiveBlock applyBackingRule() {
        return applicationStrategy.applyRule(enclosingBlock, this);
    }

    public String getApplicationDescription() {
        return applicationStrategy.getDescription();
    }

    public ParserRuleContext getDefiningContext() {
        return definingCtx;
    }
}