package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.StatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

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