package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.RuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCSwap extends VCRuleBackedStat {

    private final PExp left, right;

    public VCSwap(ParserRuleContext ctx,
                  VCAssertiveBlockBuilder block,
                  RuleApplicationStrategy apply,
                  PExp left,
                  PExp right) {
        super(ctx, block, apply);
        this.left = left;
        this.right = right;
    }

    @NotNull
    public PExp getLeft() {
        return left;
    }

    @NotNull
    public PExp getRight() {
        return right;
    }

    @NotNull
    @Override
    public VCSwap copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCSwap(definingCtx, b, applicationStrategy, left, right);
    }

    @Override
    public String toString() {
        return left + " :=: " + right + ";";
    }
}
