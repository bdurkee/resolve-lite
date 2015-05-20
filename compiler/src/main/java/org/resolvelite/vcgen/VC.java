package org.resolvelite.vcgen;

import org.resolvelite.proving.Antecedent;
import org.resolvelite.proving.Consequent;

/**
 * Represents an immutable vc (verification condition), which takes the form
 * of a mathematical implication.
 */
public class VC {

    /**
     * A human-readable name for the VC used for debugging purposes.
     */
    private final String name;

    /**
     * This is set to true to indicate that this vc is not the
     * original version of the vc with 'name'--rather, it was derived from a
     * vc named 'name' (or derived from a vc derived from a vc named 'name').
     */
    private final boolean derived;

    private final Antecedent antecedent;
    private final Consequent consequent;

    public VC(String name, Antecedent antecedent, Consequent consequent) {
        this(name, antecedent, consequent, false);
    }

    public VC(String name, Antecedent antecedent, Consequent consequent,
            boolean derived) {
        this.name = name;
        this.antecedent = antecedent;
        this.consequent = consequent;
        this.derived = derived;
    }

    public String getName() {
        String result = name;
        if ( derived ) {
            result += " (modified)";
        }
        return result;
    }

    public String getSourceName() {
        return name;
    }

    public Antecedent getAntecedent() {
        return antecedent;
    }

    public Consequent getConsequent() {
        return consequent;
    }

    @Override public String toString() {
        String retval =
                "========== " + getName() + " ==========\n" + antecedent
                        + "  -->\n" + consequent;
        return retval;
    }
}
