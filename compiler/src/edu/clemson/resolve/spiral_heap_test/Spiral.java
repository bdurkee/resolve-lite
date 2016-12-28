package edu.clemson.resolve.spiral_heap_test;

public interface Spiral<T> {

    public void lengthen(T e);
    public T shorten();

    public boolean atCenter();
    public boolean atEnd();
    public boolean atEdge();

    public void moveToCenter();
    public void moveToEnd();

    public void spiralOut();
    public void spiralIn();

    public void hopOut();
    public int hopIn();

    public int lengthOf();
    public T swapLabel(T x);

    public T getLabel();

    public T putLabel(T lab);

}
