package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;

public class VCCall extends VCRuleBackedStat {

    private final PApply callExp;

    public VCCall(ParserRuleContext ctx,
                  VCAssertiveBlock.VCAssertiveBlockBuilder block,
                  VCStatRuleApplicationStrategy apply,
                  PExp... e) {
        super(ctx, block, apply, e);
        this.callExp = (PApply) e[0];
    }

    public PApply getCallExp() {
        return callExp;
    }

}
