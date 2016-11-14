package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * An interface modeling sequents. Specifically, such sequents are of the form
 * <code>S ==&gt; T</code> where {@code S} and {@code T} are sets of well formed formulas (wffs).
 *
 * @author dtwelch
 */
public interface Sequent {

    @NotNull
    public Collection<PExp> getLeftFormulas();

    @NotNull
    public Collection<PExp> getRightFormulas();

    @NotNull
    public Sequent addRight(@NotNull PExp formula);

    @NotNull
    public Sequent addLeft(@NotNull PExp formula);

    /**
     * Returns {@code true} <strong>iff</strong> this is an identity axiom, that is, a sequent
     * of the kind <code>S,H ==&gt; H</code>.
     *
     * @return {@code true} iff this is an identity axiom.
     */
    public abstract boolean isIdentityAxiom();
}
