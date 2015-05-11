package org.resolvelite.proving;

import org.resolvelite.proving.absyn.PExp;

public class Antecedent extends ImmutableConjuncts {

    public static final Antecedent EMPTY = new Antecedent();

    public Antecedent(PExp e) {
        super(e);
    }

    public Antecedent(Iterable<PExp> i) {
        super(i);
    }

    private Antecedent() {
        super();
    }
}
