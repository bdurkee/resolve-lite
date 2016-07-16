package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.Model;
import edu.clemson.resolve.codegen.Model.OutputModelObject;
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

    private final PExp antecedent, consequent;

    public VC(int number, PExp antecedent, PExp consequent) {
        this.number = number;
        this.antecedent = antecedent;
        this.consequent = consequent;

        this.explanation = consequent.getVCExplanation();
        this.location = consequent.getVCLocation();
    }

    /** Same thing as {@link #getNumber} but gives back a string instead */
    @NotNull
    public String getName() {
        return Integer.toString(number);
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
    public PExp getAntecedent() {
        return antecedent;
    }

    @NotNull
    public PExp getConsequent() {
        return consequent;
    }

    @Override public String toString() {
        String retval =
                "========== " + getName() + " ==========\n" + antecedent
                            + "  -->\n" + consequent;
        return retval;
    }
}
