package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCCall extends VCRuleBackedStat {

    private final PApply progCall;

    public VCCall(@NotNull ParserRuleContext ctx,
                  @NotNull VCAssertiveBlockBuilder block,
                  @NotNull VCStatRuleApplicationStrategy apply,
                  @NotNull PApply progCall) {
        super(ctx, block, apply);
        this.progCall = progCall;
    }

    public PApply getProgCallExp() {
        return progCall;
    }

    @NotNull
    public VCCall copyWithBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCCall(getDefiningContext(), b, applicationStrategy, progCall);
    }
}
