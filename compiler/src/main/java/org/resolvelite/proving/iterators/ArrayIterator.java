package org.resolvelite.proving.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator<E> implements Iterator<E> {

    private final E[] array;

    private final int firstUnincludedIndex;

    private int cursor = 0;

    public ArrayIterator(E[] array) {
        this(array, 0, array.length);
    }

    public ArrayIterator(E[] array, int start, int length) {
        this.array = array;

        this.cursor = start;
        this.firstUnincludedIndex = cursor + length;
    }

    @Override public boolean hasNext() {
        return cursor < firstUnincludedIndex;
    }

    @Override public E next() {
        E retval;
        try {
            retval = array[cursor];
        }
        catch (IndexOutOfBoundsException ex) {
            throw new NoSuchElementException();
        }
        cursor++;
        return retval;
    }

    @Override public void remove() {
        throw new UnsupportedOperationException();
    }

}
