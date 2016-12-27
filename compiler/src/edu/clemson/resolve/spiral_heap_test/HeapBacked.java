package edu.clemson.resolve.spiral_heap_test;

import java.util.function.Function;

public class HeapBacked<T> implements Prioritizer<T> {

    private final Function<T, Boolean> gtr;
    private final Spiral<T> heap = new LinkedListBackedSpiral<T>(2);
    private boolean isAccepting, fullyOrdered = false;

    public HeapBacked(Function<T, Boolean> gtr) {
        this.gtr = gtr;
    }

    @Override
    public void addEntry(T x) {
        heap.moveToCenter();
        heap.moveToEnd();
        heap.lengthen(x);
        this.fullyOrdered = false;

        while (true) {
            if (heap.atCenter()) { fullyOrdered = true; break; };
            int subsectNum = heap.hopIn();

            //if subsectNum < 1 do exit
            //SO... Think of it this way, our cursor is at the end of the spiral,
            //so everytime we hop in, we have roots of subsectors left to process (towards the left...)
        }
    }

    @Override
    public boolean changeMode() {
        return isAccepting;
    }

    @Override
    public T removeSmallest(T s) {
        T result = heap.shorten();
        heap.moveToCenter();
        heap.swapLabel(s);
        fixPosition();
        return result;
    }

    @Override
    public int totalCount() {
        return heap.lengthOf();
    }

    @Override
    public boolean acceptingEntries() {
        return isAccepting;
    }

    private void fixPosition() {

    }
    @Override
    public String toString() {
        return heap.toString();
    }
}
