package edu.clemson.resolve;

import edu.clemson.resolve.spiral_heap_test.LinkedListBackedSpiral;
import edu.clemson.resolve.spiral_heap_test.Spiral;
import org.junit.Assert;
import org.junit.Test;

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
