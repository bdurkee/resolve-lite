package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.ChangeApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class VCChanging extends VCRuleBackedStat {

    private final Set<PSymbol> changingVariables = new LinkedHashSet<>();

    public VCChanging(ParserRuleContext ctx, VCAssertiveBlockBuilder block, Set<PSymbol> variables) {
        super(ctx, block, new ChangeApplicationStrategy());
        this.changingVariables.addAll(variables);
    }

    @NotNull
    public Set<PSymbol> getChangingVariables() {
        return changingVariables;
    }

    @NotNull
    @Override
    public VCRuleBackedStat copyWithEnclosingBlock(
            @NotNull VCAssertiveBlockBuilder b) {
        return new VCChanging(definingCtx, b, changingVariables);
    }

    @Override
    public String toString() {
        return "Changing " + Utils.join(changingVariables, ", ") + ";";
    }
}