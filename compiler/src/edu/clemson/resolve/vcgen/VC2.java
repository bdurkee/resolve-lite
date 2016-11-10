package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * A simple wrapper class that pairs a sequent with additional information such as location,
 * vc explanation, line number, etc.
 */
public final class VC2 {

    /** A human-readable name for the VC; used for debugging purposes. */
    private final Token location;
    private final String explanation;
    private final Sequent sequent;
    private final int number;

    public VC2(Token location, int number, String explanation, @NotNull Sequent sequent) {
        this.sequent = sequent;
        this.location = location;
        this.explanation = explanation;
        this.number = number;
    }

    @NotNull
    public Token getLocation() {
        return location;
    }

    @NotNull
    public String getExplanation() {
        return explanation;
    }

    public int getNumber() {
        return number;
    }

    @NotNull
    public Sequent getSequent() {
        return sequent;
    }

    public boolean isObviouslyTrue() {
        for (PExp e : sequent.getRightFormulas()) {
            if (!e.isLiteralTrue()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String retval = "//Vc #" + number + ": " + explanation + " (" + location.getLine() + ")" + "\n";
        int i = 1;
        boolean first = true;
        for (PExp e : sequent.getLeftFormulas()) {
            if (first) {
                retval += e.toString(false);
                first = false;
            }
            else {
                retval += ",\n" + e.toString(false);
            }
        }
        retval += "\n⊢\n";
        first = true;
        for (PExp e : sequent.getRightFormulas()) {
            if (first) {
                retval += e.toString(false);
                first = false;
            }
            else {
                retval += ",\n" + e.toString(false);
            }
        }
        return retval;
    }
}
