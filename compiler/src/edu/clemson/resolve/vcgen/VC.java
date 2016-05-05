package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.proving.Antecedent;
import edu.clemson.resolve.proving.Consequent;
import edu.clemson.resolve.proving.absyn.PExp;

/** Represents an immutable vc (verification condition), which takes the form of a mathematical implication. */
public class VC extends OutputModelObject {

    /** A human-readable name for the VC; used for debugging purposes. */
    public final String name;

    /**
     * This is set to true to indicate that this vc is not the original version of the vc with 'name'--rather, it
     * was derived from a vc named 'name' (or derived from a vc derived from a vc named 'name').
     */
    public final boolean derived;

    public final Antecedent antecedent;
    public final Consequent consequent;

    public VC(String name, PExp antecedent, PExp consequent) {
        this(name, new Antecedent(antecedent), new Consequent(consequent), false);
    }

    public VC(String name, Antecedent antecedent, Consequent consequent) {
        this(name, antecedent, consequent, false);
    }

    public VC(String name, Antecedent antecedent, Consequent consequent, boolean derived) {
        this.name = name;
        this.antecedent = antecedent;
        this.consequent = consequent;
        this.derived = derived;
    }

    public String getName() {
        String result = name;
        if (derived) result += " (modified)";
        return result;
    }

    /*@Override public String toString() {
        String retval =
                "========== " + getNameToken() + " ==========\n" + antecedent
                            + "  -->\n" + consequent;
        return retval;
    }*/
}
