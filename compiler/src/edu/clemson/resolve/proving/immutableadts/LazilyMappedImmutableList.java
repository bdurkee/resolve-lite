package edu.clemson.resolve.proving.immutableadts;

import edu.clemson.resolve.proving.iterators.ImmutableIterator;

import java.util.Iterator;
import java.util.function.Function;

public class LazilyMappedImmutableList<T, R> extends AbstractImmutableList<R> {

    private final ImmutableList<T> myOriginalList;
    private final R[] myMappedCache;

    private final Function<T, R> myMapping;

    /**
     * Constructs a new immutable list based on {@code original} in which each
     * entry in this new list will be the sister entry in that original list,
     * filtered through {@code m}.
     * <p>
     * {@code m} must represent a functional mapping--that is, if
     * {@code x.equals(y)}, then {@code m.apply(x).equals(m.apply(y)} in all
     * cases, otherwise the resulting list may appear to "mutate" to the
     * client, despite the original underlying list remaining unchanged.</p>
     *
     * @param original The original list.
     * @param m        The mapping to apply to each entry.
     */
    @SuppressWarnings("unchecked")
    public LazilyMappedImmutableList(
            ImmutableList<T> original, Function<T, R> m) {
        myOriginalList = original;
        myMapping = m;
        myMappedCache = (R[]) new Object[myOriginalList.size()];
    }

    @Override
    public ImmutableList<R> tail(int startIndex) {
        return new LazilyMappedImmutableList<T, R>(myOriginalList
                .tail(startIndex), myMapping);
    }

    @Override
    public ImmutableList<R> head(int length) {
        return new LazilyMappedImmutableList<T, R>(myOriginalList.head(length),
                myMapping);
    }

    @Override
    public R get(int index) {
        R result = myMappedCache[index];
        if ( result==null ) {
            result = myMapping.apply(myOriginalList.get(index));
            myMappedCache[index] = result;
        }
        return result;
    }

    @Override
    public Iterator<R> iterator() {
        return new ImmutableIterator<R>(new CacheCheckingIterator());
    }

    @Override
    public int size() {
        return myOriginalList.size();
    }

    private class CacheCheckingIterator implements Iterator<R> {

        private Iterator<T> myOriginalIterator = myOriginalList.iterator();
        private int myIndex = 0;

        @Override
        public boolean hasNext() {
            return myOriginalIterator.hasNext();
        }

        @Override
        public R next() {
            R result = myMappedCache[myIndex];
            T nextOriginalElement = myOriginalIterator.next();
            if ( result==null ) {
                result = myMapping.apply(nextOriginalElement);
                myMappedCache[myIndex] = result;
            }
            myIndex++;
            return result;
        }

        @Override
        public void remove() {
            myOriginalIterator.remove();
        }
    }
}
