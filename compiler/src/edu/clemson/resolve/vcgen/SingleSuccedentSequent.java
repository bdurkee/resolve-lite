package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * An interface modeling a sequents with a single succedent. Specifically, sequents of the form
 * <code>{S} ==&gt; T</code> where {@code S} is a set of mathematical expressions and {@code T} is a propositional
 * formula.
 */
public interface SingleSuccedentSequent {

    @NotNull
    public Collection<PExp> getLeftFormulas();

    /**
     * Returns {@code true} iff {@code this} sequent contains the specified expression in its left hand
     * side; {@code false} otherwise.
     *
     * @param formula the wff being searched for
     * @return {@code true} if the left hand side of the sequent contains the specified
     * expression
     */
    public boolean containsFormula(PExp formula);

    /**
     * Returns the expression in the right hand side of {@code this} sequent or {@code null} if the right hand side of
     * this sequent is empty.
     *
     * @return the formula on the right or <code>null</code>.
     */
    @Nullable
    public PExp getRight();
}
