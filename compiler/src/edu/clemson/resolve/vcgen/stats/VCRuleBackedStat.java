package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public abstract class VCRuleBackedStat {

    final ParserRuleContext definingCtx;
    final VCStatRuleApplicationStrategy applicationStrategy;
    final VCAssertiveBlockBuilder enclosingBlock;

    public VCRuleBackedStat(ParserRuleContext ctx,
                            VCAssertiveBlockBuilder block,
                            VCStatRuleApplicationStrategy apply) {
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
    public AssertiveBlock applyBackingRule() {
        return applicationStrategy.applyRule(enclosingBlock, this);
    }

    public String getApplicationDescription() {
        return applicationStrategy.getDescription();
    }

    public ParserRuleContext getDefiningContext() {
        return definingCtx;
    }
}