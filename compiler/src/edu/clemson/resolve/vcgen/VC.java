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
    private final List<PExp> antecedent, consequent;
    private final List<PExp> antecedentPieces = new ArrayList<>();

    public VC(int number, PExp antecedent, PExp consequent) {
        this.number = number;
        this.antecedent = null; //antecedent;
        this.consequent = null; //consequent;

        this.explanation = consequent.getVCExplanation();
        this.location = consequent.getVCLocation();
        this.antecedentPieces.addAll(antecedent.splitIntoConjuncts());
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

    public PExp getAntecedent() {
        return null;
    }

    public PExp getConsequent() {
        return null;
    }

    @Override public String toString() {
       /* Token location = consequent.getVCLocation();
        String explanation = consequent.getVCExplanation();

        String retval = "========== " + getName() + " ==========\n";
        if (location != null && explanation != null) {
            retval = retval + consequent.getVCExplanation() + " (" + location.getLine() + ")\n";
        }
        int i = 1;
        for (PExp e : antecedentPieces) {
            retval += i + ". " + e.toString(false) + "\n";
            i++;
        }
        retval += "⊢\n" + consequent.toString(false);
        return retval;
        */
       return null;
    }
}
