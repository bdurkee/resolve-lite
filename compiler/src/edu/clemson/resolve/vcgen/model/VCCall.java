package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCCall extends VCRuleBackedStat {

    public VCCall(@NotNull ParserRuleContext ctx,
                  @NotNull VCAssertiveBlock.VCAssertiveBlockBuilder block,
                  @NotNull VCStatRuleApplicationStrategy apply,
                  @NotNull PApply exp) {
        super(ctx, block, apply, exp);
    }

    public PApply getCallExp() {
        return (PApply) statComponents.get(0);
    }

}
