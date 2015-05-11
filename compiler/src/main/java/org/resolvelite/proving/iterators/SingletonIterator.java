package org.resolvelite.proving.iterators;

import java.util.Iterator;

public class SingletonIterator<T> implements Iterator<T> {

    private final T element;
    private boolean returned = false;

    public SingletonIterator(T element) {
        this.element = element;
    }

    @Override public boolean hasNext() {
        return !returned;
    }

    @Override public T next() {
        returned = true;
        return element;
    }

    @Override public void remove() {
        throw new UnsupportedOperationException();
    }

}
