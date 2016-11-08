package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.ListBackedSequent;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.ConfirmApplicationStrategy;
import edu.clemson.resolve.vcgen.Sequent;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

//"A confirm just adds another sequent to the list"
public class VCConfirm extends VCRuleBackedStat {

    private final Set<Sequent> sequents = new LinkedHashSet<>();

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
    public Set<Sequent> getSequents() {
        return sequents;
    }

    @NotNull
    public VCConfirm withSequentFormulaSubstitution(PExp s, PExp t) {
        Map<PExp, PExp> substitutions = new HashMap<>();
        substitutions.put(s, t);
        return withSequentFormulaSubstitution(substitutions);
    }

    @NotNull
    public VCConfirm withSequentFormulaSubstitution(Map<PExp, PExp> s) {
        List<Sequent> newSequents = new LinkedList<>();
        for (Sequent sequent : sequents) {
            newSequents.add(new ListBackedSequent(
                    Utils.apply(sequent.getLeftFormulas(), e->e.substitute(s)),
                    Utils.apply(sequent.getRightFormulas(), e->e.substitute(s))));
        }
        return new VCConfirm(definingCtx, enclosingBlock, newSequents);
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
