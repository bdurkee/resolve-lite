package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * An interface modeling sequents. Specifically, sequents of the form
 * <code>S ==&gt; T</code> where {@code S} and {@code T} are sets (collections) of well formed formulas (wffs).
 *
 * @author dtwelch
 */
public interface Sequent {

    @NotNull
    public Collection<PExp> getLeftFormulas();

    /**
     * Returns the formulas constituting the right hand side of {@code this} sequent. If the right hand
     * side is empty, returns an <em>empty</em> collection.
     *
     * @return the left-hand side of this sequent or <code>null</code>.
     */
    @NotNull
    public Collection<PExp> getRightFormulas();

    /**
     * Returns a sequent with the specified formula added to the right hand side of the sequent.
     *
     * @param wff the formula to add in the right hand side of the sequent.
     */
    @NotNull
    public Sequent addRight(@NotNull PExp wff);

    /**
     * Add the specified formula to the left hand side of this sequent.
     *
     * @param wff the formula to add.
     */
    @NotNull
    public Sequent addLeft(@NotNull PExp wff);

    /**
     * Returns {@code true} <strong>iff</strong> this is an identity axiom, that is, a sequent
     * of the kind <code>S,H ==&gt; H</code>.
     *
     * @return {@code true} iff this is an identity axiom.
     */
    public abstract boolean isIdentityAxiom();
}
