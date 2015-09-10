package edu.clemson.resolve.proving;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.immutableadts.ArrayBackedImmutableList;
import edu.clemson.resolve.proving.immutableadts.EmptyImmutableList;
import edu.clemson.resolve.proving.immutableadts.ImmutableList;

import java.io.IOException;
import java.util.*;

public class ImmutableConjuncts implements Iterable<PExp> {

    public static final ImmutableConjuncts EMPTY = new ImmutableConjuncts();
    private final ImmutableList<PExp> myConjuncts;

    private final int myConjunctsSize;

    private Set<String> myCachedSymbolNames;
    private Set<PSymbol> myCachedQuantifiedVariables;
    private List<PExp> myCachedFunctionApplications;

    /**
     * Creates a new {@code ImmutableConjuncts} whose conjuncts are the
     * top-level conjuncts of {@code e}.
     *
     * @param e The {@code PExp} to break into top-level conjuncts.
     */
    public ImmutableConjuncts(PExp e) {
        this(e.splitIntoConjuncts());
        myCachedSymbolNames = e.getSymbolNames();
        myCachedQuantifiedVariables = e.getQuantifiedVariables();
        myCachedFunctionApplications = e.getFunctionApplications();
    }

    /**
     * Creates a new {@code ImmutableConjuncts} whose conjuncts are deep copies
     * of the {@code PExp}s in {@code exps}.
     *
     * @param exps A list of {@code PExps} to make the conjuncts of the newly-
     *             created {@code ImmutableConjuncts}.
     */
    public ImmutableConjuncts(Iterable<PExp> exps) {

        if (exps instanceof ImmutableConjuncts) {
            //Performance hack: if exps is an ImmutableConjuncts, we can safely
            //just steal it's internal list of conjuncts--after all, that list
            //is immutable.
            ImmutableConjuncts expsAsImmutableConjuncts =
                    (ImmutableConjuncts) exps;
            myConjuncts = expsAsImmutableConjuncts.myConjuncts;
            myCachedSymbolNames = expsAsImmutableConjuncts.myCachedSymbolNames;
            myCachedQuantifiedVariables =
                    expsAsImmutableConjuncts.myCachedQuantifiedVariables;
            myCachedFunctionApplications =
                    expsAsImmutableConjuncts.myCachedFunctionApplications;
        }
        else {
            List<PExp> newExps = new ArrayList<>();
            exps.forEach(newExps::add);
            myConjuncts = new ArrayBackedImmutableList<>(newExps);
        }
        myConjunctsSize = myConjuncts.size();
    }

    /**
     * Private constructor for making a "blank" {@code ImmutableConjuncts} for
     * creating singleton subtypes.
     */
    protected ImmutableConjuncts() {
        List<PExp> empty = Collections.emptyList();
        myConjuncts = new ArrayBackedImmutableList<>(empty);
        myConjunctsSize = 0;
    }

    /**
     * Private constructor for making an {@code ImmutableConjuncts}
     * given an immutable list of conjuncts.  Note that expressions in the
     * immutable list must be considered immutable and have been run through
     * defensiveCopy!  Just a little performance hack.
     */
    protected ImmutableConjuncts(ImmutableList<PExp> l) {
        myConjuncts = l;
        myConjunctsSize = myConjuncts.size();
    }

    /**
     * Private constructor for making an {@code ImmutableConjuncts}
     * given an immutable list of conjuncts.  Note that expressions in the
     * immutable list must be considered immutable and have been run through
     * defensiveCopy!  Just a little performance hack.</p>
     */
    protected ImmutableConjuncts(PExp[] exps, int length) {
        myConjuncts = new ArrayBackedImmutableList<>(exps, length);
        myConjunctsSize = length;
    }

    /**
     * Returns a copy of this set of conjuncts with very obviously
     * {@code true} conjuncts removed.  Examples are the actual "true"
     * value and equalities with the same thing on the left and right side.
     */
    public ImmutableConjuncts eliminateObviousConjuncts() {
        PExp[] workingSpace = new PExp[myConjuncts.size()];

        int curIndex = 0;
        PExp curExp;
        for (PExp myConjunct : myConjuncts) {
            curExp = myConjunct;

            if (!curExp.isObviouslyTrue()) {
                workingSpace[curIndex] = curExp;
                curIndex++;
            }
        }

        return new ImmutableConjuncts(workingSpace, curIndex);
    }

    /**
     * Returns a copy of this set of conjuncts with redundant conjuncts
     * collapsed.
     *
     * @return A copy of {@code this} with redundant conjuncts removed.
     */
    public ImmutableConjuncts eliminateRedundantConjuncts() {

        int runStart = 0, runLength = 0;
        ImmutableList<PExp> newConjuncts = new EmptyImmutableList<>();

        HashSet<PExp> hashedConjuncts = new HashSet<>();
        Iterator<PExp> conjunctsIter = myConjuncts.iterator();
        PExp curConjunct;
        boolean unique;
        while (conjunctsIter.hasNext()) {
            curConjunct = conjunctsIter.next();

            unique = hashedConjuncts.add(curConjunct);

            if (unique) {
                runLength++;
            }
            else {
                newConjuncts =
                        appendConjuncts(newConjuncts, runStart, runLength);

                //Any new run will start AFTER this element.
                runStart += runLength + 1;
                runLength = 0;
            }
        }
        newConjuncts = appendConjuncts(newConjuncts, runStart, runLength);
        return new ImmutableConjuncts(newConjuncts);
    }

    private ImmutableList<PExp> appendConjuncts(ImmutableList<PExp> original,
                                                int startIndex, int length) {
        ImmutableList<PExp> retval;
        switch (length) {
            case 0:
                retval = original;
                break;
            case 1:
                retval = original.appended(myConjuncts.get(startIndex));
                break;
            default:
                retval = original.appended(myConjuncts.subList(startIndex, length));
        }
        return retval;
    }

    /**
     * Treats the provided List of {@code PExp}s as a boolean expression
     * that is the <em>and</em> of each of the elements in that iterable object.
     * Tests if that boolean expression contains equal conjuncts in the same
     * order as {@code this}.
     *
     * @param otherConjuncts The set of conjuncts to test for ordered
     * 						 equality.
     *
     * @return True <strong>iff</strong> the given expression is order-equal.
     */
    public boolean orderEqual(Iterable<PExp> otherConjuncts) {

        boolean retval = true;

        Iterator<PExp> elements = myConjuncts.iterator();
        Iterator<PExp> otherElements = otherConjuncts.iterator();
        while (retval && elements.hasNext() && otherElements.hasNext()) {
            retval = elements.next().equals(otherElements.next());
        }
        return retval && !elements.hasNext() && !otherElements.hasNext();
    }

    /**
     * Tests if each expression in the given {@code ImmutableConjuncts}
     * has an equal expression in {@code this}, and <em>visa versa</em>.
     *
     * @param o The set of conjuncts to test against {@code this}.
     * @return True <strong>iff</strong> the given set of conjuncts is equal.
     */
    public boolean equals(Object o) {
        boolean retval = (o instanceof Iterable<?>);

        if (retval) {
            Iterable<?> conjuncts = (Iterable<?>) o;

            retval =
                    oneWayEquals(conjuncts, this)
                            && oneWayEquals(this, conjuncts);
        }
        return retval;
    }

    public List<PExp> getMutableCopy() {
        List<PExp> retval = new ArrayList<>();
        Iterator<PExp> conjuncts = myConjuncts.iterator();
        PExp e;
        while (conjuncts.hasNext()) {
            e = conjuncts.next();
            retval.add(e);
        }
        return retval;
    }

    /**
     * Answers the question, "Does each {@code PExp} in {@code query} have an
     * equal {@code PExp} in {@code base}?"
     *
     * @param query expressions to test
     * @param base expressions to test against
     *
     * @return the answer
     */
    private static boolean oneWayEquals(Iterable<?> query, Iterable<?> base) {

        boolean retval = true;
        Iterator<?> queryIterator = query.iterator();

        Object curQueryExp;
        while (retval && queryIterator.hasNext()) {
            curQueryExp = queryIterator.next();
            retval = containsEqual(base, curQueryExp);
        }
        return retval;
    }

    private static boolean containsEqual(Iterable<?> source, Object e) {
        boolean retval = false;
        Iterator<?> sourceIterator = source.iterator();
        while (!retval && sourceIterator.hasNext()) {
            retval = sourceIterator.next().equals(e);
        }
        return retval;
    }

    /**
     * Returns {@code true} <strong>iff</strong> at least one of the
     * conjuncts in {@code this} is equal to {@code e}.
     *
     * @param e The {@code PExp} to test for equality.
     *
     * @return True <strong>iff</strong> {@code this} contains an
     *         equal conjunct.
     *
     * @throws NullPointerException If {@code e == null}.
     */
    public boolean containsEqual(PExp e) {
        return containsEqual(this, e);
    }

    public String toString() {
        String retval = "";

        boolean first = true;
        for (PExp e : this) {
            if (!first) retval += " and \n";
            retval += (e.toString());
            first = false;
        }
        if (retval.equals("")) retval = "True";
        return retval;
    }

    public void processStringRepresentation(PExpListener visitor, Appendable a) {
        try {
            boolean first = true;
            for (PExp e : this) {
                if (!first) {
                    a.append(" and \n");
                }
                e.processStringRepresentation(visitor, a);
                first = false;
            }
            if (first) {
                a.append("True");
            }
            a.append("\n");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public Iterator<PExp> iterator() {
        return myConjuncts.iterator();
    }

    /**
     * Returns a copy of {@code this} with all sub-expressions
     * equal to some key in {@code mapping} replaced with the corresponding
     * value.
     *
     * @param mapping The mapping to use for substituting.
     *
     * @return A new {@code ImmutableConjuncts} with the mapping applied.
     */
    public ImmutableConjuncts substitute(Map<PExp, PExp> mapping) {

        List<PExp> retvalConjuncts = new LinkedList<PExp>();
        Iterator<PExp> conjuncts = myConjuncts.iterator();
        PExp c;
        while (conjuncts.hasNext()) {
            c = conjuncts.next();
            retvalConjuncts.add(c.substitute(mapping));
        }
        return new ImmutableConjuncts(retvalConjuncts);
    }

    public ImmutableConjuncts overwritten(int index, PExp newConjunct) {
        return new ImmutableConjuncts(myConjuncts.set(index, newConjunct));
    }

    public ImmutableConjuncts inserted(int index, ImmutableConjuncts c) {
        return new ImmutableConjuncts(myConjuncts.insert(index, c.myConjuncts));
    }

    /**
     * Returns a copy of {@code this} with {@code e}s appended at
     * the end of the list of conjuncts.
     *
     * @param e The expression to append.
     *
     * @return An {@code ImmutableConjuncts} with size {@code this.size() + 1}
     * whose first {@code this.size()} conjuncts are copies of {@code this}'s
     * conjuncts, and whose last conjunct is a copy of {@code e}.
     */
    public ImmutableConjuncts appended(PExp e) {
        return new ImmutableConjuncts(myConjuncts.appended(e));
    }

    /**
     * Returns a copy of {@code this} with the {@code PExp}s from {@code i}
     * appended at the end of the list of conjuncts.
     *
     * @param i The expressions to append.
     *
     * @return An {@code ImmutableConjuncts} whose first {@code this.size()}
     * conjuncts are copies of {@code this}'s conjuncts, and whose other
     * conjuncts are copies of the {@code PExp}s in {@code i}, in order.
     */
    public ImmutableConjuncts appended(Iterable<PExp> i) {
        ImmutableConjuncts retval;

        //TODO: This is dangerous if we change the implementation of
        //       ImmutableConjuncts to a list of lists
        if (i instanceof ImmutableConjuncts) {
            ImmutableList<PExp> iConjuncts =
                    ((ImmutableConjuncts) i).myConjuncts;

            if (iConjuncts.size() == 0) {
                retval = this;
            }
            else {
                //Performance hack: if i is an ImmutableConjuncts, we can safely
                //just steal it's internal list of conjuncts--after all, that
                //list is immutable.
                ImmutableList<PExp> newExps = myConjuncts.appended(iConjuncts);

                retval = new ImmutableConjuncts(newExps);
            }
        }
        else {
            Iterator<PExp> iIterator = i.iterator();

            if (iIterator.hasNext()) {
                retval = new ImmutableConjuncts(i);
            }
            else {
                retval = this;
            }
        }

        return retval;
    }

    /**
     * Returns a new {@code ImmutableConjuncts} derived from this one,
     * whose conjuncts are all the conjuncts of {@code this} except the
     * {@code index}th.
     *
     * @param indexToRemove The index to remove.
     * @return A new {@code ImmutableConjuncts} without the {@code idex}th one.
     *
     * @throws IndexOutOfBoundsException If the provided index is out of bounds.
     */
    public ImmutableConjuncts removed(int indexToRemove) {
        return new ImmutableConjuncts(myConjuncts.removed(indexToRemove));
    }

    /**
     * Returns a new {@code ImmutableConjuncts} derived from this one,
     * whose conjuncts are the conjuncts of {@code this} starting at the
     * {@code start}th conjunct (zero-based) and containing the following
     * {@code length} conjuncts.  If {@code start + length} exceeds
     * {@code this.size()}, the returned {@code ImmutableConjuncts}
     * will contain the following {@code size - start} conjuncts instead.
     * <p>
     * If {@code start >= this.size()}, will return an {@code ImmutableConjuncts}
     * with {@code size() == 0}.</p>
     *
     * @param start The index of the first conjunct to include (zero-based).
     * @param length The number of conjuncts from that point to include.
     *
     * @return A new {@code ImmutableConjuncts} containing those conjuncts
     *         starting at {@code start} and extending to include the next
     *         {@code length} conjuncts, or all the remaining conjuncts if
     *         {@code length} extends past the end of the list.
     *
     * @throws IndexOutOfBoundsException If either {@code start} or
     *             {@code length} is negative.
     */
    public ImmutableConjuncts subConjuncts(int start, int length) {

        //myConjuncts.subList is less forgiving than we are, so adjust our
        //parameters to get the desired results without an
        //IndexOutOfBoundsException
        if (start > myConjunctsSize) {
            start = myConjunctsSize;
        }
        if (start + length > myConjunctsSize) {
            length = myConjunctsSize - start;
        }
        return new ImmutableConjuncts(myConjuncts.subList(start, length));
    }

    /**
     * Returns a deep copy of the {@code index}th conjunct in {@code this}.
     *
     * @param index The index of the conjunct to retrieve, zero-based.
     * @return A deep copy of the {@code index}th conjunct in {@code this}.
     *
     * @throws IndexOutOfBoundsException If the provided index is less than zero
     *            or equal to or greater than {@code size()}.
     */
    public PExp get(int index) {
        return myConjuncts.get(index);
    }

    /**
     * Returns the number of conjuncts in {@code this}.
     *
     * @return The number of conjuncts.
     */
    public int size() {
        return myConjuncts.size();
    }

    public ImmutableConjuncts flipQuantifiers() {
        ImmutableConjuncts retval;

        PExp[] workingSpace = new PExp[myConjuncts.size()];
        boolean conjunctChanged = false;
        int curIndex = 0;
        Iterator<PExp> conjunctIter = myConjuncts.iterator();
        PExp curConjunct;

        while (conjunctIter.hasNext()) {
            curConjunct = conjunctIter.next();
            workingSpace[curIndex] = curConjunct.withQuantifiersFlipped();
            conjunctChanged |= (workingSpace[curIndex] != curConjunct);
            curIndex++;
        }

        if (conjunctChanged) {
            retval = new ImmutableConjuncts(workingSpace, myConjunctsSize);
        }
        else {
            retval = this;
        }

        return retval;
    }

    public boolean containsQuantifiedVariableNotIn(ImmutableConjuncts o) {
        Set<PSymbol> oVariables = o.getQuantifiedVariables();
        Set<PSymbol> thisVariables = this.getQuantifiedVariables();
        return !oVariables.containsAll(thisVariables);
    }

    public Set<PSymbol> getQuantifiedVariables() {
        if (myCachedQuantifiedVariables == null) {
            myCachedQuantifiedVariables = new HashSet<>();

            for (PExp myConjunct : myConjuncts) {
                myCachedQuantifiedVariables.addAll(myConjunct
                        .getQuantifiedVariables());
            }
        }

        return myCachedQuantifiedVariables;
    }

    public List<PExp> getFunctionApplications() {
        if (myCachedFunctionApplications == null) {
            myCachedFunctionApplications = new LinkedList<PExp>();

            for (PExp myConjunct : myConjuncts) {
                myCachedFunctionApplications.addAll(myConjunct
                        .getFunctionApplications());
            }
        }
        return myCachedFunctionApplications;
    }

    public Set<String> getSymbolNames() {
        if (myCachedSymbolNames == null) {
            myCachedSymbolNames = new HashSet<>();
            for (PExp myConjunct : myConjuncts) {
                myCachedSymbolNames.addAll(myConjunct
                        .getSymbolNames());
            }
        }
        return myCachedSymbolNames;
    }
}
