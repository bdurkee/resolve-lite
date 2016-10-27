package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathFunctionClssftn;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VCIfElse extends VCRuleBackedStat {

    private final List<VCRuleBackedStat> thenStmts = new ArrayList<>();
    private final List<VCRuleBackedStat> elseStmts = new ArrayList<>();

    public VCIfElse(ParserRuleContext ctx,
                    VCAssertiveBlock.VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy apply,
                    List<VCRuleBackedStat> thenStmts,
                    List<VCRuleBackedStat> elseStmts,
                    PExp progCondition) {
        super(ctx, block, apply, progCondition);
        this.thenStmts.addAll(thenStmts);
        this.elseStmts.addAll(elseStmts);
    }

    //used primarily for flipping
    public VCIfElse(VCIfElse existing, VCStatRuleApplicationStrategy apply) {
        this(existing.getDefiningContext(), existing.enclosingBlock, apply,
                existing.thenStmts, existing.elseStmts, existing.getIfCondition());
    }

    @NotNull
    public PExp getIfCondition() {
        return statComponents.get(0);
    }

    @NotNull
    public PExp negateMathCondition(PExp mathConditionToNegate) {
        DumbMathClssftnHandler g = enclosingBlock.g;
        PExp name = new PSymbol.PSymbolBuilder("⌐")
                .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, g.BOOLEAN))
                .build();
        return new PApply.PApplyBuilder(name)
                .applicationType(g.BOOLEAN)
                .arguments(mathConditionToNegate)
                .build();
    }

    @NotNull
    public VCIfElse copyWithBlock(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder b) {
        return new VCIfElse(definingCtx, b, applicationStrategy,
                Utils.apply(thenStmts, e -> e.copyWithBlock(b)),
                Utils.apply(elseStmts, e -> e.copyWithBlock(b)), getIfCondition());
    }

    @NotNull
    public List<VCRuleBackedStat> getThenStmts() {
        return thenStmts;
    }

    @NotNull
    public List<VCRuleBackedStat> getElseStmts() {
        return elseStmts;
    }
}
