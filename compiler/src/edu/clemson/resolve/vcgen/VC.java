package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.proving.Antecedent;
import edu.clemson.resolve.proving.Consequent;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Represents an immutable vc (verification condition), which takes the form of a mathematical implication. */
public final class VC extends OutputModelObject {

    /** A human-readable name for the VC; used for debugging purposes. */
    private final String name;

    private final List<VCInfo> info = new ArrayList<>();

    private final Antecedent antecedent;
    private final Consequent consequent;

    public VC(String name, PExp antecedent, PExp consequent) {
        this(name, new Antecedent(antecedent), new Consequent(consequent));
    }

    public VC(String name, Antecedent antecedent, Consequent consequent) {
        this.name = name;
        this.antecedent = antecedent;
        this.consequent = consequent;

        for (PExp c : consequent) {
            if (c.getVCLocation() != null && c.getVCExplanation() != null) {
                this.info.add(new VCInfo(c.getVCLocation(), c.getVCExplanation()));
            }
        }
    }

    /**
     * Returns information associated with the first (and typically <em>only</em>) consequent of this {@code VC}.
     *
     * @return consequent information
     */
    @NotNull public VCInfo getConsequentInfo() {
        return info.get(0);
    }

    /**
     * Returns information associated with each consequent of a given {@code VC}.
     * <p>
     * Note that currently due to the way {@link PExp#split()} works, we will only have VCs with a <em>single</em>
     * consequent, as such consider using {@link #getConsequentInfo()} instead for simplicity. This is just here
     * for the sake of generality.</p>
     *
     * @return A list of information corresponding to all consequents of this {@code VC}.
     */
    @NotNull public List<VCInfo> getAllConsequentInfo() {
        return info;
    }

    @NotNull public String getName() {
        return name;
    }

    @NotNull public Antecedent getAntecedent() {
        return antecedent;
    }

    @NotNull public Consequent getConsequent() {
        return consequent;
    }

    /*@Override public String toString() {
        String retval =
                "========== " + getNameToken() + " ==========\n" + antecedent
                            + "  -->\n" + consequent;
        return retval;
    }*/

    /**
     * Encapsulates information about a specific VC and its one or more embedded consequents.
     * <p>
     * Specifically, this object pairs the VC concrete document-locational info
     * (represented by an {@link Token} containing line number, document name, etc) with a short textual explanation
     * describing what caused the VC to be generated.
     *
     * @author dtwelch
     */
    public static class VCInfo {

        @NotNull public final String explanation;
        @NotNull public final Token location;

        public VCInfo(@NotNull Token location, @NotNull String explanation) {
            this.location = location;
            this.explanation = explanation;
        }
    }
}
