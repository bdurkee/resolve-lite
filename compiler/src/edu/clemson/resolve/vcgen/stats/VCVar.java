package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.application.VarApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCVar extends VCRuleBackedStat {

    private final ProgType type;

    public VCVar(ParserRuleContext ctx, VCAssertiveBlockBuilder block, ProgType type) {
        super(ctx, block, new VarApplicationStrategy());
        this.type = type;
    }

    @NotNull
    public ProgType getVarType() {
        return type;
    }

    @NotNull
    @Override
    public VCRuleBackedStat copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCVar(definingCtx, b, type);
    }
}
