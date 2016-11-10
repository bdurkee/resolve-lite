package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
    public int hashCode() {
        return toString().hashCode();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ListBackedSequent)) return false;
        //if contents (and length of left and right sides are the same)
        boolean leftEqual = sideEqual(left, ((ListBackedSequent) o).left);
        boolean rightEqual = sideEqual(right, ((ListBackedSequent) o).right);
        return leftEqual && rightEqual;
    }

    private boolean sideEqual(List<PExp> l1, List<PExp> l2) {
        boolean retval = true;
        Iterator<PExp> l1Iter = l1.iterator();
        Iterator<PExp> l2Iter = l2.iterator();
        while (retval && l1Iter.hasNext() && l2Iter.hasNext()) {
            retval = l1Iter.next().equals(l2Iter.next());
        }
        return retval && !(l1Iter.hasNext() || l2Iter.hasNext());
    }

}
