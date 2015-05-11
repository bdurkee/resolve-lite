package org.resolvelite.proving.immutableadts;

import java.util.Iterator;

public class ImmutableListSubview<E> extends AbstractImmutableList<E> {

    private final ArrayBackedImmutableList<E> baseList;
    private final int subviewStart;
    private final int subviewLength;
    private final int firstAfterIndex;

    public ImmutableListSubview(ArrayBackedImmutableList<E> baseList,
            int start, int length) {

        //TODO : These defensive checks can be taken out for efficiency once
        //       we're satisfied that ImmutableLists works correctly.
        if ( start + length > baseList.size() ) {
            throw new IllegalArgumentException("View exceeds source bounds.");
        }

        if ( length < 0 ) {
            throw new IllegalArgumentException("Negative length.");
        }

        if ( start < 0 ) {
            throw new IllegalArgumentException("Negative start.");
        }

        this.baseList = baseList;
        this.subviewStart = start;
        this.subviewLength = length;
        this.firstAfterIndex = subviewStart + subviewLength;
    }

    @Override public E get(int index) {
        if ( index < 0 || index >= firstAfterIndex ) {
            throw new IndexOutOfBoundsException();
        }
        return baseList.get(index + subviewStart);
    }

    @Override public ImmutableList<E> head(int length) {
        if ( length > subviewLength ) {
            throw new IndexOutOfBoundsException();
        }
        return new ImmutableListSubview<E>(baseList, subviewStart, length);
    }

    @Override public Iterator<E> iterator() {
        return baseList.subsequenceIterator(subviewStart, subviewLength);
    }

    @Override public int size() {
        return subviewLength;
    }

    @Override public ImmutableList<E> tail(int startIndex) {
        if ( startIndex < 0 || startIndex > subviewLength ) {
            throw new IndexOutOfBoundsException();
        }
        return new ImmutableListSubview<E>(baseList, startIndex + subviewStart,
                subviewLength - startIndex);
    }

}
