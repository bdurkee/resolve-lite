package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;

public class VCIf extends VCRuleBackedStat {

    public VCIf(ParserRuleContext ctx,
                VCAssertiveBlock.VCAssertiveBlockBuilder block,
                VCStatRuleApplicationStrategy apply, PExp... e) {
        super(ctx, block, apply, e);
    }

}
