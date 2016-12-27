package edu.clemson.resolve.spiral_heap_test;

public interface Prioritizer<T> {

    public void addEntry(T x);

    public T removeSmallest(T s);

    public int totalCount();

    public boolean changeMode();
   // public T removeAnEntry();

    public boolean acceptingEntries();
}
