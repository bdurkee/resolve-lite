package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.ConfirmApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All verification confirm statements should have some explanation as to why it was included/where it arose from.
 * <p>
 * For instance, a (final) confirm initiated by some
 * {@link ResolveParser.OperationProcedureDeclContext} should contain a textual blurb saying that it comes from
 * the ensures clause of the operation procedure's name. This is what {@link #getExplanation()} should return.
 */
public class VCConfirm extends VCRuleBackedStat {

    @Nullable
    private final String explanation;

    public VCConfirm(@NotNull ParserRuleContext definingCtx,
                     @NotNull VCAssertiveBlockBuilder block,
                     @Nullable String explanation,
                     @NotNull PExp e) {
        super(definingCtx, block, new ConfirmApplicationStrategy(), e);
        this.explanation = explanation;
    }

    @Nullable
    public String getExplanation() {
        return explanation;
    }

    @NotNull
    public PExp getConfirmExp() {
        return statComponents.get(0);
    }
}
