package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.proving.Antecedent;
import edu.clemson.resolve.proving.Consequent;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents an immutable vc (verification condition), which takes the form of a mathematical implication. */
public final class VC extends OutputModelObject {

    /** A human-readable name for the VC; used for debugging purposes. */
    private final String name;

    private final List<Pair<Token, String>> explanations = new ArrayList<>();

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
                this.explanations.add(new Pair<>(c.getVCLocation(), c.getVCExplanation()));
            }
        }
    }

    @NotNull public List<Pair<Token, String>> getConsequentExplanations() {
        return explanations;
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
}
