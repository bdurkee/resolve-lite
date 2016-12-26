package edu.clemson.resolve.spiral_heap_test;

import edu.clemson.resolve.misc.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LinkedListBackedSpiral<T> implements Spiral<T> {

    private final LinkedList<T> contents = new LinkedList<T>();
    private int k, currentPosition;

    public LinkedListBackedSpiral(int k) {
        this.k = k;
    }

    @Override
    public void lengthen(T e) {
        contents.add(e);
    }

    @Override
    public T shorten() {
        T result = contents.removeLast();
        currentPosition = contents.size() - 1;
        return result;
    }

    @Override
    public void hopOut() {
        currentPosition = k * currentPosition + 1;
    }

    @Override
    public void hopIn() {
        currentPosition = (currentPosition - 1) / 2;
    }

    @Override
    public T swapLabel(T x) {
        T result = contents.get(currentPosition);
        contents.set(currentPosition, x);
        return result;
    }

    @Override
    public boolean atCenter() {
        return currentPosition == 0;
    }

    @Override
    public boolean atEdge() {
        return k * currentPosition >= contents.size();
    }

    @Override
    public void spiralOut() {
        currentPosition++;
    }

    @Override
    public void spiralIn() {
        currentPosition--;
    }

    @Override
    public String toString() {
        return Utils.join(contents, ", ");
    }
}
