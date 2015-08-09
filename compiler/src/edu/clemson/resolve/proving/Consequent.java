package edu.clemson.resolve.proving;

import edu.clemson.resolve.proving.absyn.PExp;

import java.util.Map;

public class Consequent extends ImmutableConjuncts {

    public Consequent(PExp e) {
        super(e);
    }

    public Consequent(Iterable<PExp> i) {
        super(i);
    }

    @Override public Consequent substitute(Map<PExp, PExp> mapping) {
        ImmutableConjuncts genericRetval = super.substitute(mapping);
        return new Consequent(genericRetval);
    }

    @Override public Consequent appended(Iterable<PExp> i) {
        ImmutableConjuncts genericRetval = super.appended(i);
        return new Consequent(genericRetval);
    }

    @Override public Consequent eliminateObviousConjuncts() {
        ImmutableConjuncts genericRetval = super.eliminateObviousConjuncts();
        return new Consequent(genericRetval);
    }

    @Override public Consequent removed(int index) {
        ImmutableConjuncts genericRetval = super.removed(index);
        return new Consequent(genericRetval);
    }

    @Override public Consequent eliminateRedundantConjuncts() {
        ImmutableConjuncts genericRetval = super.eliminateRedundantConjuncts();
        return new Consequent(genericRetval);
    }

    public Antecedent assumed() {
        return new Antecedent(this);
    }
}
