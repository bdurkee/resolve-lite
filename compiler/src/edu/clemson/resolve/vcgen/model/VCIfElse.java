package edu.clemson.resolve.vcgen.model;

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

    private final List<VCRuleBackedStat> bodyStatements = new ArrayList<>();

    /**
     * {@code true} if this instance represents the if part (with body); false if {@code this} represents
     * the else part.
     */
    private final boolean forIf;

    public VCIfElse(ParserRuleContext ctx,
                    VCAssertiveBlock.VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy apply,
                    List<VCRuleBackedStat> bodyStats,
                    boolean forIf,
                    PExp progCondition) {
        super(ctx, block, apply, progCondition);
        this.forIf = forIf;
        this.bodyStatements.addAll(bodyStats);
    }

    public boolean isForIf() {
        return forIf;
    }

    @NotNull
    public PExp getIfCondition() {
        return statComponents.get(0);
    }

    @NotNull
    public PExp negateMathCondition(PExp mathematicalCondition) {
        DumbMathClssftnHandler g = getEnclosingBlock().g;
        PExp name = new PSymbol.PSymbolBuilder("⌐")
                .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, g.BOOLEAN))
                .build();
        return new PApply.PApplyBuilder(name)
                .applicationType(g.BOOLEAN)
                .arguments(mathematicalCondition)
                .build();
    }

    @NotNull
    public List<VCRuleBackedStat> getBodyStatements() {
        return bodyStatements;
    }
}
