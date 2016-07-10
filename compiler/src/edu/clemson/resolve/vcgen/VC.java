package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.proving.Antecedent;
import edu.clemson.resolve.proving.Consequent;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;

/** Represents an immutable vc (verification condition), which takes the form of a mathematical implication. */
public final class VC extends OutputModelObject {

    /** A human-readable name for the VC; used for debugging purposes. */
    private final int number;
    private final String explanation;

    private final Token location;

    private final Antecedent antecedent;
    private final Consequent consequent;

    public VC(int number, PExp antecedent, PExp consequent) {
        this(number, new Antecedent(antecedent), new Consequent(consequent));
    }

    public VC(int number, Antecedent antecedent, Consequent consequent) {
        this.number = number;
        this.antecedent = antecedent;
        this.consequent = consequent;

        if (consequent.size() != 1) {
            throw new UnsupportedOperationException("Only VCs with a single consequent are supported at the moment, " +
                    "the {@link PExp:split()} method is likely at fault here");
        }
        PExp consequentExp = consequent.get(0);
        this.explanation = consequentExp.getVCExplanation();
        this.location = consequentExp.getVCLocation();
    }

    public int getNumber() {
        return number;
    }

    @NotNull
    public String getExplanation() {
        return explanation;
    }

    @NotNull
    public Token getLocation() {
        return location;
    }

    @NotNull
    public Antecedent getAntecedent() {
        return antecedent;
    }

    @NotNull
    public Consequent getConsequent() {
        return consequent;
    }

    /*@Override public String toString() {
        String retval =
                "========== " + getNameToken() + " ==========\n" + antecedent
                            + "  -->\n" + consequent;
        return retval;
    }*/
}
