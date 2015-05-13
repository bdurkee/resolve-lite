package org.resolvelite.proving;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpVisitor;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.immutableadts.ArrayBackedImmutableList;
import org.resolvelite.proving.immutableadts.EmptyImmutableList;
import org.resolvelite.proving.immutableadts.ImmutableList;

import java.io.IOException;
import java.util.*;

public class ImmutableConjuncts implements Iterable<PExp> {

    public static final ImmutableConjuncts EMPTY = new ImmutableConjuncts();

    private final ImmutableList<PExp> conjuncts;
    private final int conjunctsSize;

    private Set<String> cachedSymbolNames;
    private Set<PSymbol> cachedQuantifiedVariables;
    private List<PExp> cachedFunctionApplications;

    /**
     * Creates a new {@code ImmutableConjuncts} whose conjuncts are
     * the top-level conjuncts of {@code e}.
     * 
     * @param e The {@code PExp} to break into top-level conjuncts.
     */
    public ImmutableConjuncts(PExp e) {
        this(e.splitIntoConjuncts());
        this.cachedSymbolNames = e.getSymbolNames();
        this.cachedQuantifiedVariables = e.getQuantifiedVariables();
        this.cachedFunctionApplications = e.getFunctionApplications();
    }

    public ImmutableConjuncts(Iterable<PExp> exps) {

        if ( exps instanceof ImmutableConjuncts ) {
            //Performance hack: if exps is an ImmutableConjuncts, we can safely
            //just steal it's internal list of conjuncts--after all, that list
            //is immutable.
            ImmutableConjuncts expsAsImmutableConjuncts =
                    (ImmutableConjuncts) exps;

            conjuncts = expsAsImmutableConjuncts.conjuncts;
            cachedSymbolNames = expsAsImmutableConjuncts.cachedSymbolNames;
            cachedQuantifiedVariables =
                    expsAsImmutableConjuncts.cachedQuantifiedVariables;
            cachedFunctionApplications =
                    expsAsImmutableConjuncts.cachedFunctionApplications;
        }
        else {
            List<PExp> newExps = new LinkedList<PExp>();
            for (PExp e : exps) {
                newExps.add(e);
            }
            conjuncts = new ArrayBackedImmutableList<PExp>(newExps);
        }
        conjunctsSize = conjuncts.size();
    }

    /**
     * Private constructor for making a "blank" {@code ImmutableConjuncts} for
     * creating singleton subtypes.
     */
    protected ImmutableConjuncts() {
        List<PExp> empty = Collections.emptyList();
        conjuncts = new ArrayBackedImmutableList<PExp>(empty);
        conjunctsSize = 0;
    }

    /**
     * Private constructor for making an {@code ImmutableConjuncts} given an
     * immutable list of conjuncts. Note that expressions in the
     * immutable list must be considered immutable and have been run through
     * defensiveCopy! Just a little performance hack.
     */
    protected ImmutableConjuncts(ImmutableList<PExp> l) {
        conjuncts = l;
        conjunctsSize = conjuncts.size();
    }

    /**
     * Private constructor for making an {@code ImmutableConjuncts} given an
     * immutable list of conjuncts. Note that expressions in the immutable list
     * must be considered immutable and have been run through defensiveCopy!
     * Just a little performance hack.
     */
    protected ImmutableConjuncts(PExp[] exps, int length) {
        conjuncts = new ArrayBackedImmutableList<PExp>(exps, length);
        conjunctsSize = length;
    }

    /**
     * Returns a copy of this set of conjuncts with very obviously {@code true}
     * conjuncts removed. Examples are the actual "true"
     * value and equalities with the same thing on the left and right side.
     */
    public ImmutableConjuncts eliminateObviousConjuncts() {
        PExp[] workingSpace = new PExp[conjuncts.size()];

        int curIndex = 0;
        PExp curExp;
        Iterator<PExp> iter = conjuncts.iterator();
        while (iter.hasNext()) {
            curExp = iter.next();

            if ( !curExp.isObviouslyTrue() ) {
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
        Iterator<PExp> conjunctsIter = conjuncts.iterator();
        PExp curConjunct;
        boolean unique;

        while (conjunctsIter.hasNext()) {
            curConjunct = conjunctsIter.next();
            unique = hashedConjuncts.add(curConjunct);
            if ( unique ) {
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
            retval = original.appended(conjuncts.get(startIndex));
            break;
        default:
            retval = original.appended(conjuncts.subList(startIndex, length));
        }

        return retval;
    }

    /**
     * Treats the provided List of {@code PExp}s as a boolean expression
     * that is the and of each of the elements in that iterable object.
     * Tests if that boolean expression contains equal conjuncts in the same
     * order as {@code this}.
     * 
     * @param otherConjuncts The set of conjuncts to test for ordered
     *        equality.
     * 
     * @return True iff the given expression is order-equal.
     */
    public boolean orderEqual(Iterable<PExp> otherConjuncts) {

        boolean retval = true;

        Iterator<PExp> elements = conjuncts.iterator();
        Iterator<PExp> otherElements = otherConjuncts.iterator();
        while (retval && elements.hasNext() && otherElements.hasNext()) {
            retval = elements.next().equals(otherElements.next());
        }
        return retval && !elements.hasNext() && !otherElements.hasNext();
    }

    public boolean equals(Object o) {
        boolean retval = (o instanceof Iterable<?>);

        if ( retval ) {
            Iterable<?> conjuncts = (Iterable<?>) o;
            retval =
                    oneWayEquals(conjuncts, this)
                            && oneWayEquals(this, conjuncts);
        }
        return retval;
    }

    /**
     * Returns a list of the conjuncts in {@code this}.
     * 
     * @return A mutable copy of {@code this}.
     */
    public List<PExp> getMutableCopy() {
        List<PExp> retval = new LinkedList<PExp>();

        Iterator<PExp> conjuncts = this.conjuncts.iterator();
        PExp e;
        while (conjuncts.hasNext()) {
            e = conjuncts.next();

            retval.add(e);
        }
        return retval;
    }

    /**
     * Answers the question, "Does each {@code PExp} in {@code query} have
     * an equal {@code PExp} in {@code base}?"
     * 
     * @param query The expressions to test.
     * @param base The expressions to test against.
     * @return The answer.
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

    /**
     * Returns {@code true} iff at least one of the conjuncts in {@code source}
     * is equal to {@code e}.
     * 
     * @param source A list of conjuncts to test against.
     * @param e The {@code PExp}s to test for equality.
     * 
     * @return True iff {@code source} contains an equal conjunct.
     * 
     * @throws NullPointerException If {@code source} or {@code e} is null.
     */
    private static boolean containsEqual(Iterable<?> source, Object e) {
        boolean retval = false;

        Iterator<?> sourceIterator = source.iterator();
        while (!retval && sourceIterator.hasNext()) {
            retval = sourceIterator.next().equals(e);
        }
        return retval;
    }

    /**
     * Returns {@code true} iff at least one of the conjuncts in {@code this}
     * is equal to {@code e}.
     * 
     * @param e The {@code PExp} to test for equality.
     * 
     * @return {@code true} iff {@code this} contains an equal conjunct.
     * 
     * @throws NullPointerException If {@code e} is null.
     */
    public boolean containsEqual(PExp e) {
        return containsEqual(this, e);
    }

    public String toString() {
        String retval = "";

        boolean first = true;
        for (PExp e : this) {
            if ( !first ) {
                retval += " and \n";
            }
            retval += (e.toString());
            first = false;
        }
        if ( retval.equals("") ) {
            retval = "True";
        }
        retval += "\n";
        return retval;
    }

    public void processStringRepresentation(PExpVisitor visitor, Appendable a) {
        try {
            boolean first = true;
            for (PExp e : this) {
                if ( !first ) {
                    a.append(" and \n");
                }
                e.processStringRepresentation(visitor, a);
                first = false;
            }
            if ( first ) {
                a.append("True");
            }
            a.append("\n");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public Iterator<PExp> iterator() {
        return conjuncts.iterator();
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

        Iterator<PExp> conjuncts = this.conjuncts.iterator();
        PExp c;
        while (conjuncts.hasNext()) {
            c = conjuncts.next();
            retvalConjuncts.add(c.substitute(mapping));
        }

        return new ImmutableConjuncts(retvalConjuncts);
    }

    public ImmutableConjuncts overwritten(int index, PExp newConjunct) {
        return new ImmutableConjuncts(conjuncts.set(index, newConjunct));
    }

    public ImmutableConjuncts inserted(int index, ImmutableConjuncts c) {
        return new ImmutableConjuncts(conjuncts.insert(index, c.conjuncts));
    }

    /**
     * Returns a copy of {@code this} with {@code e}s appended at the end of the
     * list of conjuncts.
     * 
     * @param e The expression to append.
     * 
     * @return An {@code ImmutableConjuncts} with size {@code this.size() + 1}
     *         whose first {@code this.size()} conjuncts are copies of
     *         {@code this}'s conjuncts, and whose last conjunct is a copy of
     *         {@code e}.
     */
    public ImmutableConjuncts appended(PExp e) {
        return new ImmutableConjuncts(conjuncts.appended(e));
    }

    /**
     * Returns a copy of {@code this} with the {@code PExp}s from {@code i}
     * appended at the end of the list of conjuncts.
     * 
     * @param i The expressions to append.
     * 
     * @return An {@code ImmutableConjuncts} whose first {@code this.size()}
     *         conjuncts are copies of {@code this}'s conjuncts, and whose other
     *         conjuncts are copies of the {@code PExp}s in {@code i}, in order.
     */
    public ImmutableConjuncts appended(Iterable<PExp> i) {
        ImmutableConjuncts retval;

        //FIXME: This is dangerous if we change the implementation of
        //       ImmutableConjuncts to a list of lists
        if ( i instanceof ImmutableConjuncts ) {
            ImmutableList<PExp> iConjuncts = ((ImmutableConjuncts) i).conjuncts;

            if ( iConjuncts.size() == 0 ) {
                retval = this;
            }
            else {
                //Performance hack: if i is an ImmutableConjuncts, we can safely
                //just steal it's internal list of conjuncts--after all, that
                //list is immutable.
                ImmutableList<PExp> newExps = conjuncts.appended(iConjuncts);

                retval = new ImmutableConjuncts(newExps);
            }
        }
        else {
            Iterator<PExp> iIterator = i.iterator();

            if ( iIterator.hasNext() ) {
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
     * 
     * @return A new {@code ImmutableConjuncts} without the {@code index}th one.
     * 
     * @throws IndexOutOfBoundsException If the provided index is out of bounds.
     */
    public ImmutableConjuncts removed(int indexToRemove) {
        return new ImmutableConjuncts(conjuncts.removed(indexToRemove));
    }

    /**
     * Returns a new {@code ImmutableConjuncts} derived from this one,
     * whose conjuncts are the conjuncts of {@code this} starting at the
     * {@code start}th conjunct (zero-based) and containing the following
     * {@code length} conjuncts. If {@code start + length} exceeds
     * {@code this.size()}, the returned {@code ImmutableConjuncts} will contain
     * the following {@code size - start} conjuncts instead.
     * 
     * If {@code start >= this.size()}, will return an
     * {@code ImmutableConjuncts} with {@code size() == 0}.
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
     *         {@code length} is negative.
     */
    public ImmutableConjuncts subConjuncts(int start, int length) {

        //conjuncts.subList is less forgiving than we are, so adjust our
        //parameters to get the desired results without an
        //IndexOutOfBoundsException
        if ( start > conjunctsSize ) {
            start = conjunctsSize;
        }
        if ( start + length > conjunctsSize ) {
            length = conjunctsSize - start;
        }
        return new ImmutableConjuncts(conjuncts.subList(start, length));
    }

    /**
     * Returns a deep copy of the {@code index}th conjunct in {@code index}th.
     * 
     * @param index The index of the conjunct to retrieve, zero-based.
     * 
     * @return A deep copy of the {@code index}th conjunct in {@code this}.
     * 
     * @throws IndexOutOfBoundsException If the provided index is less than zero
     *         or equal to or greater than {@code size()}.
     */
    public PExp get(int index) {
        return conjuncts.get(index);
    }

    /**
     * Returns the number of conjuncts in {@code this}.
     * 
     * @return The number of conjuncts.
     */
    public int size() {
        return conjuncts.size();
    }

    public ImmutableConjuncts flipQuantifiers() {
        ImmutableConjuncts retval;
        PExp[] workingSpace = new PExp[conjuncts.size()];

        boolean conjunctChanged = false;
        int curIndex = 0;
        Iterator<PExp> conjunctIter = conjuncts.iterator();
        PExp curConjunct;
        while (conjunctIter.hasNext()) {
            curConjunct = conjunctIter.next();
            workingSpace[curIndex] = curConjunct.flipQuantifiers();
            conjunctChanged |= (workingSpace[curIndex] != curConjunct);
            curIndex++;
        }
        if ( conjunctChanged ) {
            retval = new ImmutableConjuncts(workingSpace, conjunctsSize);
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
        if ( cachedQuantifiedVariables == null ) {
            cachedQuantifiedVariables = new HashSet<PSymbol>();

            for (PExp conjunct : conjuncts) {
                cachedQuantifiedVariables.addAll(conjunct
                        .getQuantifiedVariables());
            }
        }
        return cachedQuantifiedVariables;
    }

    public List<PExp> getFunctionApplications() {
        if ( cachedFunctionApplications == null ) {
            cachedFunctionApplications = new LinkedList<PExp>();

            for (PExp conjunct : conjuncts) {
                cachedFunctionApplications.addAll(conjunct
                        .getFunctionApplications());
            }
        }
        return cachedFunctionApplications;
    }

    public Set<String> getSymbolNames() {
        if ( cachedSymbolNames == null ) {
            cachedSymbolNames = new HashSet<String>();

            for (PExp conjunct : conjuncts) {
                cachedSymbolNames.addAll(conjunct.getSymbolNames());
            }
        }
        return cachedSymbolNames;
    }
}
