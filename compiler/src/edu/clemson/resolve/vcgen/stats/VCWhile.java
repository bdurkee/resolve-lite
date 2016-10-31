package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VCWhile extends VCRuleBackedStat {

    private final PExp progCondition, maintaining, decreasing;
    private final List<VCRuleBackedStat> body = new ArrayList<>();
    private final boolean branchSatisfied;

    public VCWhile(ParserRuleContext ctx,
                   VCAssertiveBlockBuilder block,
                   VCStatRuleApplicationStrategy apply,
                   PExp condition,
                   PExp maintaining,
                   PExp decreasing,
                   List<VCRuleBackedStat> stmts,
                   boolean satisfiedCondition) {
        super(ctx, block, apply);
        this.progCondition = condition;
        this.maintaining = maintaining;
        this.decreasing = decreasing;
        this.body.addAll(stmts);
        this.branchSatisfied = satisfiedCondition;
    }

    public boolean branchSatisfied() {
        return branchSatisfied;
    }

    @NotNull
    public PExp getInvariant() {
        return maintaining;
    }

    @Nullable
    public PExp getDecreasing() {
        return decreasing;
    }

    @NotNull
    public List<VCRuleBackedStat> getBody() {
        return body;
    }

    @NotNull
    public PExp getProgCondition() {
        return progCondition;
    }

    @NotNull
    public VCWhile withFlippedBranch(VCAssertiveBlockBuilder b) {
        return new VCWhile(definingCtx, b, applicationStrategy,
                progCondition, maintaining,
                decreasing, Utils.apply(body, e -> e.copyWithEnclosingBlock(b)), !branchSatisfied);
    }

    @NotNull
    @Override
    public VCRuleBackedStat copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCWhile(definingCtx, b, applicationStrategy,
                progCondition, maintaining,
                decreasing, Utils.apply(body, e -> e.copyWithEnclosingBlock(b)), branchSatisfied);
    }
}
