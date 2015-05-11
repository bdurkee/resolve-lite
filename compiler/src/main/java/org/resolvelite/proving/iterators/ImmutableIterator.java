package org.resolvelite.proving.iterators;

import java.util.Iterator;

/**
 * Wraps an existing {@code Iterator} and disables its {@code remove()} method,
 * ensuring that clients cannot change the contents of encapsulated lists.
 * Note that if the iterator returns mutable objects, the contained objects
 * themselves could still be changed.
 */
public class ImmutableIterator<T> implements Iterator<T> {

    private final Iterator<T> innerIterator;

    public ImmutableIterator(Iterator<T> inner) {
        innerIterator = inner;
    }

    @Override public boolean hasNext() {
        return innerIterator.hasNext();
    }

    @Override public T next() {
        return innerIterator.next();
    }

    @Override public void remove() {
        throw new UnsupportedOperationException("iterator is immutable.");
    }

}
