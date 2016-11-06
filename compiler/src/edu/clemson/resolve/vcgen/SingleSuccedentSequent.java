package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;

import java.util.Collection;

/**
 * Interface modelling a sequent of the form <code>{S} ==&gt; T</code> where
 * {@code S} is a set of mathematical expressions and {@code T} is a propositional formula.
 */
public interface SingleSuccedentSequent {

    public Collection<PExp> getAntecedents();

    /**
     * Returns {@code true} iff {@code this} sequent contains the specified expression in its
     * left hand side; {@code false} otherwise.
     *
     * @param e the wff being searched for
     * @return {@code true} if the left hand side of the sequent contains the specified
     * expression
     */
    public abstract boolean containsAntecedent(PExp e);

    /**
     * Returns the expression in the right hand side of {@code this} sequent or {@code null} if the
     * right hand side of this sequent is empty.
     *
     * @return the formula on the right or <code>null</code>.
     */
    public PExp getSuccedent();
}
