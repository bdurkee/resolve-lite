package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.RuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCCall extends VCRuleBackedStat {

    private final PApply progCall;

    public VCCall(@NotNull ParserRuleContext ctx,
                  @NotNull VCAssertiveBlockBuilder block,
                  @NotNull RuleApplicationStrategy apply,
                  @NotNull PApply progCall) {
        super(ctx, block, apply);
        this.progCall = progCall;
    }

    public PApply getProgCallExp() {
        return progCall;
    }

    @NotNull
    public VCCall copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCCall(getDefiningContext(), b, applicationStrategy, progCall);
    }

    @Override
    public String toString() {
        return progCall + ";";
    }
}
