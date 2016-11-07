package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.app.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCAssign extends VCRuleBackedStat {

    private final PExp left, right;

    public VCAssign(ParserRuleContext ctx,
                    VCAssertiveBlock.VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy apply,
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

    public boolean isFunctionAssignment() {
        return right instanceof PApply;
    }

    @NotNull
    @Override
    public VCAssign copyWithEnclosingBlock(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder b) {
        return new VCAssign(definingCtx, b, applicationStrategy, left, right);
    }

    @Override
    public String toString() {
        return left + " := " + right + ";";
    }
}