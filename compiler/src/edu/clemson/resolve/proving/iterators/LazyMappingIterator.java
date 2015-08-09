package edu.clemson.resolve.proving.iterators;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.function.Function;

/**
 * A {@code LazyMappingIterator} wraps an {@code Iterator} that iterates over
 * objects of type {@code I} and presents an interface for mapping over objects
 * of type {@code O}.  A function from {@code I} to {@code O} is used to
 * transform each object as it is requested.
 *
 * @param <I> The type of the objects in the source iterator.
 * @param <O> The type of the final objects.
 */
public final class LazyMappingIterator<I, O> implements Iterator<O> {

    private final Iterator<I> mySource;
    private final Function<I, O> myMapper;

    public LazyMappingIterator(Iterator<I> source, Function<I, O> mapper) {
        mySource = source;
        myMapper = mapper;
    }

    @Override public boolean hasNext() {
        return mySource.hasNext();
    }

    @Override public O next() {
        try {
            return myMapper.apply(mySource.next());
        }
        catch (ConcurrentModificationException cme) {
            int i = 5;
            throw new RuntimeException(cme);
        }
    }

    @Override public void remove() {
        mySource.remove();
    }
}
