package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.ConfirmApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VCConfirm extends VCRuleBackedStat {

    public VCConfirm(ParserRuleContext definingCtx, VCAssertiveBlockBuilder block, PExp e) {
        super(definingCtx, block, new ConfirmApplicationStrategy(), e);
    }

    @NotNull
    public PExp getConfirmExp() {
        return statComponents.get(0);
    }

    @NotNull
    public VCConfirm copyWithBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCConfirm(getDefiningContext(), b, getConfirmExp());
    }
}
