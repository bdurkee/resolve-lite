package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.vcgen.ListBackedSequent;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.application.ConfirmApplicationStrategy;
import edu.clemson.resolve.vcgen.Sequent;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

//"A confirm just adds another sequent to the list"
public class VCConfirm extends VCRuleBackedStat {

    private final List<Sequent> sequents = new ArrayList<>();

    public VCConfirm(ParserRuleContext definingCtx,
                     VCAssertiveBlockBuilder block,
                     Sequent startingSequent) {
        this(definingCtx, block, Collections.singletonList(startingSequent));
    }

    public VCConfirm(ParserRuleContext definingCtx,
                     VCAssertiveBlockBuilder block,
                     Collection<Sequent> sequents) {
        super(definingCtx, block, new ConfirmApplicationStrategy());
        this.sequents.addAll(sequents);
    }

    @NotNull
    public List<Sequent> getSequents() {
        return sequents;
    }

    @NotNull
    @Override
    public VCConfirm copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCConfirm(definingCtx, b, sequents);
    }

    @Override
    public String toString() {
        return "Confirm " + Utils.join(sequents, " ∧ ") + ";";
    }
}
