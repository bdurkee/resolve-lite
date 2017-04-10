package edu.clemson.resolve.spiral_heap_test;

import java.util.List;

public interface Prioritizer<T> {

    public void addEntry(T x);

    public void fullyOrder(List<T> entries);

    public T removeSmallest();

    public int totalCount();

    public boolean changeMode();
   // public T removeAnEntry();

    public boolean acceptingEntries();
}
