package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ListBackedSequent implements Sequent {

    public static final Sequent EMPTY_SEQUENT = new ListBackedSequent();

    private final List<PExp> left = new LinkedList<>();
    private final List<PExp> right = new LinkedList<>();

    private ListBackedSequent() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    /** Builds an sequent with a single succeedent and no ntecedents. */
    public ListBackedSequent(PExp right) {
        this(Collections.emptyList(), Collections.singletonList(right));
    }

    public ListBackedSequent(List<PExp> left, List<PExp> right) {
        this.left.addAll(left);
        this.right.addAll(right);
    }

    @NotNull
    @Override
    public List<PExp> getLeftFormulas() {
        return Collections.unmodifiableList(new LinkedList<>(left));
    }

    @NotNull
    @Override
    public Collection<PExp> getRightFormulas() {
        return Collections.unmodifiableList(new LinkedList<>(right));
    }

    @NotNull
    @Override
    public Collection<PExp> getAllFormulas() {
        List<PExp> combined = getLeftFormulas();
        combined.addAll(getRightFormulas());
        return combined;
    }

    @NotNull
    @Override
    public Sequent addRight(@NotNull PExp formula) {
        List<PExp> newRightFormulas = new LinkedList<>(left);
        newRightFormulas.add(formula);
        return new ListBackedSequent(getLeftFormulas(), newRightFormulas);
    }

    @NotNull
    @Override
    public Sequent addLeft(@NotNull PExp formula) {
        List<PExp> newLeftFormulas = new LinkedList<>(left);
        newLeftFormulas.add(formula);
        return new ListBackedSequent(newLeftFormulas, right);
    }

    @Override
    public String toString() {
        String leftStr = "{";
        boolean first = true;
        for (PExp antecedent : left) {
            if (first) {
                leftStr += antecedent.toString(false);
                first = false;
            }
            else {
                leftStr += ", " + antecedent.toString(false);
            }
        }
        leftStr += "}";

        String rightStr = "{";
        first = true;
        for (PExp succeedent : right) {
            if (first) {
                rightStr += succeedent.toString(false);
                first = false;
            }
            else {
                rightStr += ", " + succeedent.toString(false);
            }
        }
        rightStr += "}";
        return leftStr + " ⟹ " + rightStr;
    }
}
