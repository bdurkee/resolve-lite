package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.RuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

public abstract class VCRuleBackedStat {

    final ParserRuleContext definingCtx;
    RuleApplicationStrategy applicationStrategy;
    final VCAssertiveBlockBuilder enclosingBlock;

    public VCRuleBackedStat(ParserRuleContext ctx,
                            VCAssertiveBlockBuilder block,
                            RuleApplicationStrategy apply) {
        this.applicationStrategy = apply;
        this.enclosingBlock = block;
        this.definingCtx = ctx;
    }

    /**
     * Creates a deep copy of {@code this} statement with enclosing block {@code b}.
     *
     * @param b The enclosing {@link VCAssertiveBlockBuilder} for this statement.
     * @return a deep copy of this statement.
     */
    @NotNull
    public abstract VCRuleBackedStat copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b);

    @SuppressWarnings("unchecked")
    public VCAssertiveBlock applyBackingRule(Deque<VCAssertiveBlockBuilder> accumulator) {
        return applicationStrategy.applyRule(accumulator, enclosingBlock, this);
    }

    public String getApplicationDescription() {
        return applicationStrategy.getDescription();
    }

    public ParserRuleContext getDefiningContext() {
        return definingCtx;
    }



}