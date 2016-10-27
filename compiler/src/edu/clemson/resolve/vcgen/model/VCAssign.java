package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCAssign extends VCRuleBackedStat {

    private final PExp left, right;

    public VCAssign(ParserRuleContext ctx,
                    VCAssertiveBlock.VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy apply,
                    PExp left,
                    PExp right) {
        super(ctx, block, apply, left, right);
    }

    @NotNull
    @Override
    public VCRuleBackedStat copyWithBlock(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder b) {
        return new VCAssign(definingCtx, b, applicationStrategy, statComponents);
    }


}
