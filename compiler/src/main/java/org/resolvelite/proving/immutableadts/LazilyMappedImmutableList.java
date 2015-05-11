package org.resolvelite.proving.immutableadts;

import org.resolvelite.proving.iterators.ImmutableIterator;

import java.util.Iterator;
import java.util.function.Function;

public class LazilyMappedImmutableList<T, R> extends AbstractImmutableList<R> {

    private final ImmutableList<T> originalList;
    private final R[] mappedCache;

    private final Function<T, R> mapping;

    /**
     * Constructs a new immutable list based on {@code original}, in
     * which each entry in this new list will be the sister entry in that
     * original list, filtered through {@code m}. {@code m} must
     * represent a functional mapping--that is, if {@code x.equals(y)},
     * then {@code m.map(x).equals(m.map y)} in all cases, otherwise the
     * resulting list may appear to "mutate" to the client, despite the original
     * underlying list remaining unchanged.
     * 
     * @param original The original list.
     * @param m The mapping to apply to each entry.
     */
    @SuppressWarnings("unchecked") public LazilyMappedImmutableList(
            ImmutableList<T> original, Function<T, R> m) {

        this.originalList = original;
        this.mapping = m;
        this.mappedCache = (R[]) new Object[originalList.size()];
    }

    @Override public ImmutableList<R> tail(int startIndex) {
        return new LazilyMappedImmutableList<T, R>(
                originalList.tail(startIndex), mapping);
    }

    @Override public ImmutableList<R> head(int length) {
        return new LazilyMappedImmutableList<T, R>(originalList.head(length),
                mapping);
    }

    @Override public R get(int index) {
        R result = mappedCache[index];
        if ( result == null ) {
            result = mapping.apply(originalList.get(index));
            mappedCache[index] = result;
        }
        return result;
    }

    @Override public Iterator<R> iterator() {
        return new ImmutableIterator<R>(new CacheCheckingIterator());
    }

    @Override public int size() {
        return originalList.size();
    }

    private class CacheCheckingIterator implements Iterator<R> {

        private Iterator<T> myOriginalIterator = originalList.iterator();
        private int myIndex = 0;

        @Override public boolean hasNext() {
            return myOriginalIterator.hasNext();
        }

        @Override public R next() {
            R result = mappedCache[myIndex];

            T nextOriginalElement = myOriginalIterator.next();
            if ( result == null ) {
                result = mapping.apply(nextOriginalElement);
                mappedCache[myIndex] = result;
            }

            myIndex++;

            return result;
        }

        @Override public void remove() {
            myOriginalIterator.remove();
        }
    }
}
