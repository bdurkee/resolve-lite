package org.resolvelite.proving.iterators;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.function.Function;

/**
 * <p>
 * A <code>LazyMappingIterator</code> wraps an <code>Iterator</code> that
 * iterates over objects of type <code>I</code> and presents an interface for
 * mapping over objects of type <code>O</code>. A <code>Mapping</code> from
 * <code>I</code> to <code>O</code> is used to transform each object as it is
 * requested.
 * </p>
 * 
 * @param <I> The type of the objects in the source iterator.
 * @param <O> The type of the final objects.
 */
public final class LazyMappingIterator<I, O> implements Iterator<O> {

    private final Iterator<I> source;
    private final Function<I, O> mapper;

    public LazyMappingIterator(Iterator<I> source, Function<I, O> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override public boolean hasNext() {
        return source.hasNext();
    }

    @Override public O next() {
        try {
            return this.mapper.apply(source.next());
        }
        catch (ConcurrentModificationException cme) {
            int i = 5;
            throw new RuntimeException(cme);
        }
    }

    @Override public void remove() {
        source.remove();
    }
}
