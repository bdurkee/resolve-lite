package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathFunctionClssftn;
import edu.clemson.resolve.vcgen.ModelBuilderProto;
import edu.clemson.resolve.vcgen.application.ConditionalApplicationStrategy;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.application.ConditionalApplicationStrategy.IfApplicationStrategy;
import edu.clemson.resolve.vcgen.application.ConditionalApplicationStrategy.ElseApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VCIfElse extends VCRuleBackedStat {

    private final List<VCRuleBackedStat> thenStmts = new ArrayList<>();
    private final List<VCRuleBackedStat> elseStmts = new ArrayList<>();
    private final PExp progCondition;

    public VCIfElse(ParserRuleContext ctx,
                    VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy apply,
                    List<VCRuleBackedStat> thenStmts,
                    List<VCRuleBackedStat> elseStmts,
                    PExp progCondition) {
        super(ctx, block, apply);
        this.progCondition = progCondition;
        this.thenStmts.addAll(thenStmts);
        this.elseStmts.addAll(elseStmts);
    }

    @NotNull
    public ConditionalApplicationStrategy getOppositeConditionalStrategy() {
        return applicationStrategy instanceof IfApplicationStrategy ?
                ModelBuilderProto.ELSE_APPLICATION :
                ModelBuilderProto.IF_APPLICATION;
    }

    @NotNull
    public PExp getProgIfCondition() {
        return progCondition;
    }

    @NotNull
    public VCIfElse copyWithBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCIfElse(definingCtx, b, applicationStrategy,
                Utils.apply(thenStmts, e -> e.copyWithBlock(b)),
                Utils.apply(elseStmts, e -> e.copyWithBlock(b)), progCondition);
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
