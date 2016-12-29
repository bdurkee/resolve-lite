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
    public int hopIn() {
        int offset = (monus(currentPosition, 1)) % k;
        currentPosition = (monus(currentPosition, 1) / k);
        return offset;
    }

    private int monus(int a, int b) {
        if (a < b) return 0;
        else return a - b;
    }

    @Override
    public T swapLabel(T x) {
        T result = contents.get(currentPosition);
        contents.set(currentPosition, x);
        return result;
    }

    @Override
    public T getLabel() {
        return contents.get(currentPosition);
    }

    @Override
    public T putLabel(T lab) {
        return contents.set(currentPosition, lab);
    }

    @Override
    public boolean atCenter() {
        return currentPosition == 0;
    }

    @Override
    public boolean atEnd() {
        return currentPosition == contents.size()-1;
    }

    @Override
    public boolean atEdge() {
        return k * currentPosition >= contents.size()-1;
    }

    @Override
    public void moveToCenter() {
        currentPosition = 0;
    }

    @Override
    public void moveToEnd() {
        currentPosition = contents.size();
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
    public int lengthOf() {
        return contents.size();
    }

    @Override
    public String toString() {
        return Utils.join(contents, ", ");
    }
}
