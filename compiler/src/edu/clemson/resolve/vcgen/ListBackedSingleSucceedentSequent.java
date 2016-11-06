package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListBackedSingleSucceedentSequent implements SingleSuccedentSequent {

    private List<PExp> antecedents;
    private PExp succeedent;

    /** Builds an empty sequent. */
    public ListBackedSingleSucceedentSequent() {
        this.antecedents = new ArrayList<>();
    }

    @Override
    public List<PExp> getAntecedents() {
        return antecedents;
    }

    @Override
    public boolean containsAntecedent(PExp e) {
        return false;
    }

    @Override
    public PExp getSuccedent() {
        return succeedent;
    }
}
