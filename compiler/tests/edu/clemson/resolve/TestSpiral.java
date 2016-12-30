package edu.clemson.resolve;

import edu.clemson.resolve.spiral_heap_test.HeapBacked;
import edu.clemson.resolve.spiral_heap_test.LinkedListBackedSpiral;
import edu.clemson.resolve.spiral_heap_test.Prioritizer;
import edu.clemson.resolve.spiral_heap_test.Spiral;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestSpiral {

    /*
        2
    3        4
5      1   9      7
     */
    @Test
    public void testLengthen() {
        Spiral<Integer> s = buildExample();
        Assert.assertEquals("2, 3, 4, 5, 1, 9, 7", s.toString());
    }
    /*
        2
    3        4
5      1   9      7
     */
    @Test
    public void testHopOutAndHopIn() {
        Spiral<Integer> s = buildExample();
        s.hopOut();
        Assert.assertEquals("3", s.swapLabel(0).toString());
        s.hopOut();
        Assert.assertEquals("5", s.swapLabel(0).toString());
        s = buildExample();
        Assert.assertEquals("2, 3, 4, 5, 1, 9, 7", s.toString());
        s.hopOut();
        s.hopOut();
        s.hopIn();
        Assert.assertEquals("3", s.swapLabel(0).toString());
        s.hopIn();
        Assert.assertEquals("2", s.swapLabel(0).toString());
        s.swapLabel(2);
        s.hopOut();
        s.swapLabel(3);
        s.hopOut();
        Assert.assertEquals("2, 3, 4, 5, 1, 9, 7", s.toString());
        s.spiralOut();
        Assert.assertEquals("1", s.swapLabel(0).toString());
        s.hopIn();
        Assert.assertEquals("3", s.swapLabel(0).toString());
    }
    /*
        2
    3        4
5      1   9      7
     */
    @Test
    public void testHopIn() {
        Spiral<Integer> s = buildExample();
        s.spiralOut();
        s.spiralOut();
        Assert.assertEquals(1, s.hopIn());
        s.spiralOut();
        Assert.assertEquals(0, s.hopIn());
        s.moveToCenter();
        s.hopOut();
        s.hopOut();
        Assert.assertEquals(0, s.hopIn());
        s.hopOut();
        s.spiralOut();
        Assert.assertEquals(1, s.hopIn());

        //switch to arity 3
        s = buildExample(3);
        s.spiralOut();
        Assert.assertEquals(0, s.hopIn());
        Assert.assertEquals("2", s.swapLabel(0).toString());
        s.swapLabel(2);
        Assert.assertEquals("2, 3, 4, 5, 1, 9, 7", s.toString());

        s.hopOut();
        s.spiralOut();
        Assert.assertEquals(1, s.hopIn());
        Assert.assertEquals("2", s.swapLabel(0).toString());
        s.swapLabel(2);
        Assert.assertEquals("2, 3, 4, 5, 1, 9, 7", s.toString());

        s.hopOut();
        s.spiralOut();
        s.spiralOut();
        Assert.assertEquals(2, s.hopIn());
        Assert.assertEquals("2", s.swapLabel(0).toString());
        s.swapLabel(2);
        Assert.assertEquals("2, 3, 4, 5, 1, 9, 7", s.toString());
    }

    @Test
    public void testAtEnd() {
        Spiral<Integer> s = new LinkedListBackedSpiral<>(2);
        s.lengthen(-10);
        s.lengthen(5);
        s.lengthen(-2);
        s.lengthen(6);
        s.lengthen(20);
        s.lengthen(10);
        Assert.assertEquals("-10, 5, -2, 6, 20, 10", s.toString());

        Assert.assertEquals(false, s.atEdge());
        s.spiralOut();
        Assert.assertEquals(false, s.atEdge());
        s.spiralOut();
        Assert.assertEquals(false, s.atEdge());
        s.spiralOut();
        Assert.assertEquals(true, s.atEdge());
        s.spiralOut();
        Assert.assertEquals(true, s.atEdge());
        s.spiralOut();
        Assert.assertEquals(true, s.atEdge());
        s.hopIn();
        Assert.assertEquals(false, s.atEdge());
    }

    @Test
    public void testPrioritizerAddEntry1() {
        Prioritizer<Integer> p = new HeapBacked<>((x, y) -> x < y);
        p.addEntry(5);
        Assert.assertEquals("5", p.toString());

        p.addEntry(2);
        Assert.assertEquals("2, 5", p.toString());

        p.addEntry(10);
        Assert.assertEquals("2, 5, 10", p.toString());

        p.addEntry(6);
        Assert.assertEquals("2, 5, 10, 6", p.toString());

        p.addEntry(20);
        Assert.assertEquals("2, 5, 10, 6, 20", p.toString());

        p.addEntry(1);
        Assert.assertEquals("1, 5, 2, 6, 20, 10", p.toString());
    }

    @Test
    public void testPrioritizerAddEntry2() {
        Prioritizer<Integer> p = new HeapBacked<>((x, y) -> x < y);
        p.addEntry(10);
        Assert.assertEquals("10", p.toString());

        p.addEntry(9);
        Assert.assertEquals("9, 10", p.toString());

        p.addEntry(8);
        Assert.assertEquals("8, 10, 9", p.toString());

        p.addEntry(7);
        Assert.assertEquals("7, 8, 9, 10", p.toString());

        p.addEntry(5);
        Assert.assertEquals("5, 7, 9, 10, 8", p.toString());

        p.addEntry(4);
        Assert.assertEquals("4, 7, 5, 10, 8, 9", p.toString());

        p.addEntry(3);
        Assert.assertEquals("3, 7, 4, 10, 8, 9, 5", p.toString());

        p.addEntry(2);
        Assert.assertEquals("2, 3, 4, 7, 8, 9, 5, 10", p.toString());

        p.addEntry(1);
        Assert.assertEquals("1, 2, 4, 3, 8, 9, 5, 10, 7", p.toString());

        p.addEntry(6);
        Assert.assertEquals("1, 2, 4, 3, 6, 9, 5, 10, 7, 8", p.toString());
    }

    //Woo! works! :)
    @Test
    public void testBuildHeap() {
        //List<Integer> e = Stream.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1).collect(Collectors.toList());
        List<Integer> e = Stream.of(31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15,
                14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1).collect(Collectors.toList());

        Prioritizer<Integer> p = new HeapBacked<>((x, y) -> x < y);
        p.fullyOrder(e);
        Assert.assertEquals("1, 9, 2, 13, 10, 5, 3, 15, 14, 11, 21, 7, 6, 4, 17, 16, 24, 28, 23, 12, 22, 27, 30, 8, " +
                "20, 26, 19, 31, 18, 25, 29", p.toString());
        //System.out.println(p.toString());
    }

    private Spiral<Integer> buildExample() {
        return buildExample(2);
    }

    private Spiral<Integer> buildExample(int arity) {
        Spiral<Integer> s = new LinkedListBackedSpiral<>(arity);
        s.lengthen(2);
        s.lengthen(3);
        s.lengthen(4);
        s.lengthen(5);
        s.lengthen(1);
        s.lengthen(9);
        s.lengthen(7);
        return s;
    }
}
