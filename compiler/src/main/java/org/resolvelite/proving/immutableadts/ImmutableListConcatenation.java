package org.resolvelite.proving.immutableadts;

import org.resolvelite.proving.iterators.ChainingIterator;

import java.util.Iterator;

public class ImmutableListConcatenation<E> extends AbstractImmutableList<E> {

    private final ImmutableList<E> firstList;
    private final int firstListSize;

    private final ImmutableList<E> secondList;
    private final int secondListSize;

    private final int totalSize;

    public ImmutableListConcatenation(ImmutableList<E> firstList,
            ImmutableList<E> secondList) {

        this.firstList = firstList;
        firstListSize = this.firstList.size();

        this.secondList = secondList;
        secondListSize = this.secondList.size();

        totalSize = firstListSize + secondListSize;
    }

    @Override public E get(int index) {
        E retval;

        if ( index < firstListSize ) {
            retval = firstList.get(index);
        }
        else {
            retval = secondList.get(index - firstListSize);
        }

        return retval;
    }

    @Override public ImmutableList<E> head(int length) {
        ImmutableList<E> retval;

        if ( length <= firstListSize ) {
            retval = firstList.head(length);
        }
        else {
            retval =
                    new ImmutableListConcatenation<E>(firstList,
                            secondList.head(length - firstListSize));
        }

        return retval;
    }

    @Override public Iterator<E> iterator() {
        return new ChainingIterator<E>(firstList.iterator(),
                secondList.iterator());
    }

    @Override public int size() {
        return totalSize;
    }

    @Override public ImmutableList<E> subList(int startIndex, int length) {
        return tail(startIndex).head(length);
    }

    @Override public ImmutableList<E> tail(int startIndex) {
        ImmutableList<E> retval;
        if ( startIndex < firstListSize ) {
            retval =
                    new ImmutableListConcatenation<E>(
                            firstList.tail(startIndex), secondList);
        }
        else {
            retval = secondList.tail(startIndex - firstListSize);
        }
        return retval;
    }
}
