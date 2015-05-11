package org.resolvelite.proving.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ChainingIterator<T> implements Iterator<T> {

    private Iterator<T> startIterator;
    private boolean startHasNext = true;
    private Iterator<T> endIterator;
    private boolean lastFromStart;

    public ChainingIterator(Iterator<T> start, Iterator<T> end) {

        //TODO : This can be removed to increase performance
        if ( start == null || end == null ) {
            throw new IllegalArgumentException();
        }
        this.startIterator = start;
        this.endIterator = end;
    }

    public boolean hasNext() {
        if ( startHasNext ) {
            startHasNext = startIterator.hasNext();
        }

        return (startHasNext || endIterator.hasNext());
    }

    public T next() {
        T retval;

        if ( !hasNext() ) {
            throw new NoSuchElementException();
        }

        if ( startHasNext ) {
            retval = startIterator.next();
            lastFromStart = true;
        }
        else {
            retval = endIterator.next();
            lastFromStart = false;
        }

        return retval;
    }

    public void remove() {
        if ( lastFromStart ) {
            startIterator.remove();
        }
        else {
            endIterator.remove();
        }
    }

}
