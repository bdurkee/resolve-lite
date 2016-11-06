package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.ConfirmApplicationStrategy;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCConfirm extends VCRuleBackedStat {

    private final PExp confirm;

    public VCConfirm(ParserRuleContext definingCtx, VCAssertiveBlockBuilder block, PExp confirm) {
        super(definingCtx, block, new ConfirmApplicationStrategy());
        this.confirm = confirm;
    }

    @NotNull
    public PExp getConfirmExp() {
        return confirm;
    }

    @NotNull
    public VCConfirm copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCConfirm(definingCtx, b, confirm);
    }

    @Override
    public String toString() {
        return "Confirm " + confirm.toString(false) + ";";
    }
}
