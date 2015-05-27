package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.misc.Utils;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.application.StatRuleApplicationStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VCRuleBackedStat extends OutputModelObject {

    protected final ParserRuleContext definingCtx;
    protected final List<PExp> statComponents = new ArrayList<>();
    protected final StatRuleApplicationStrategy applicationStrategy;
    protected final VCAssertiveBlockBuilder enclosingBlock;

    public VCRuleBackedStat(ParserRuleContext ctx,
            VCAssertiveBlockBuilder block, StatRuleApplicationStrategy apply,
            PExp... e) {
        this.statComponents.addAll(Arrays.asList(e));
        this.applicationStrategy = apply;
        this.enclosingBlock = block;
        this.definingCtx = ctx;
    }

    public String getText() {
        if ( definingCtx != null ) return Utils.getRawText(definingCtx);
        return "";
    }

    public List<PExp> getStatComponents() {
        return statComponents;
    }

    public AssertiveBlock reduce() {
        return applicationStrategy.applyRule(enclosingBlock, statComponents);
    }

    public String getApplicationDescription() {
        return applicationStrategy.getDescription();
    }

    public VCAssertiveBlockBuilder getEnclosingBlock() {
        return enclosingBlock;
    }
}
