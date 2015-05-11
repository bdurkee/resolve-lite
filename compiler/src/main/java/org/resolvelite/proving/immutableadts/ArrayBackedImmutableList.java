package org.resolvelite.proving.immutableadts;

import org.resolvelite.proving.iterators.ArrayIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ArrayBackedImmutableList<E> extends AbstractImmutableList<E> {

    private final E[] elements;
    private final int elementsLength;

    private final int hashCode;

    @SuppressWarnings("unchecked") public ArrayBackedImmutableList(Iterable<E> i) {
        List<E> tempList = new ArrayList<E>();

        for (E e : i) {
            tempList.add(e);
        }

        elements = (E[]) tempList.toArray();
        elementsLength = elements.length;
        hashCode = calculateHashCode();
    }

    public ArrayBackedImmutableList(E[] i) {
        elementsLength = i.length;
        elements = Arrays.copyOf(i, elementsLength);
        hashCode = calculateHashCode();
    }

    public ArrayBackedImmutableList(E[] i, int length) {
        elementsLength = length;
        elements = Arrays.copyOf(i, length);
        hashCode = calculateHashCode();
    }

    private int calculateHashCode() {
        int result = 0;
        for (E e : elements) {
            result += e.hashCode() * 74;
        }
        return result;
    }

    @Override public E get(int index) {
        return elements[index];
    }

    @Override public ImmutableList<E> head(int length) {
        return new ImmutableListSubview<E>(this, 0, length);
    }

    @Override public Iterator<E> iterator() {
        return new ArrayIterator<E>(elements);
    }

    public Iterator<E> subsequenceIterator(int start, int length) {
        return new ArrayIterator<E>(elements, start, length);
    }

    @Override public int size() {
        return elementsLength;
    }

    @Override public ImmutableList<E> tail(int startIndex) {
        return new ImmutableListSubview<E>(this, startIndex, elementsLength
                - startIndex);
    }

    @Override public int hashCode() {
        return hashCode;
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof ArrayBackedImmutableList);

        if ( result ) {
            ArrayBackedImmutableList oAsABIL = (ArrayBackedImmutableList) o;

            result = (elementsLength == oAsABIL.size());

            if ( result ) {
                int i = 0;
                while (i < elementsLength && result) {
                    result = (elements[i].equals(oAsABIL.get(i)));
                    i++;
                }
            }
        }

        return result;
    }
}
