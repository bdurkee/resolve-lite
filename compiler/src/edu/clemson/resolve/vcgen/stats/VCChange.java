package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.application.ChangeApplicationStrategy;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class VCChange extends VCRuleBackedStat {

    private final Set<PSymbol> changeVariables = new LinkedHashSet<>();

    public VCChange(ParserRuleContext ctx,
                    VCAssertiveBlockBuilder block,
                    Set<PSymbol> variables) {
        super(ctx, block, new ChangeApplicationStrategy());
        this.changeVariables.addAll(variables);
    }

    @NotNull
    public Set<PSymbol> getChangeVariables() {
        return changeVariables;
    }

    @NotNull
    @Override
    public VCRuleBackedStat copyWithEnclosingBlock(
            @NotNull VCAssertiveBlockBuilder b) {
        return new VCChange(definingCtx, b, changeVariables);
    }
}
