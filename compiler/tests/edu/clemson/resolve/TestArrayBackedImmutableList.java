package edu.clemson.resolve;

import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.immutableadts.ArrayBackedImmutableList;
import edu.clemson.resolve.proving.immutableadts.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestArrayBackedImmutableList extends BaseTest {

    @Test public void testAppended() throws Exception {
        List<String> mList = new ArrayList<>();
        mList.add("cat");
        mList.add("dog");
        mList.add("fish");
        mList.add("box");
        ImmutableList<String> l = new ArrayBackedImmutableList<String>(mList);
        Assert.assertEquals(l.size(), 4);
        Assert.assertEquals("[cat, dog, fish, box]", l.toString());
        mList.add("foo");
        Assert.assertEquals(l.size(), 4);
        Assert.assertEquals("[cat, dog, fish, box]", l.toString());
        l = l.appended(mList);
        Assert.assertEquals("[cat, dog, fish, box, cat, dog, " +
                "fish, box, foo]", l.toString());
        Assert.assertEquals(l.size(), 9);
        String x = null;
        l = l.appended(x);
        Assert.assertEquals("[cat, dog, fish, box, cat, dog," +
                " fish, box, foo, null]", l.toString());
        Assert.assertEquals(l.size(), 10);

        mList = new ArrayList<>();  //reinit the list

        //things better not change
        Assert.assertEquals("[cat, dog, fish, box, cat, dog," +
                " fish, box, foo, null]", l.toString());
        Assert.assertEquals(l.size(), 10);

        //append l to l2 from an *empty* immutablelist, l2 (mList is now empty)
        ImmutableList<String> l2 = new ArrayBackedImmutableList<String>(mList);
        l2 = l2.appended(l);
        Assert.assertEquals("[cat, dog, fish, box, cat, dog," +
                " fish, box, foo, null]", l2.toString()); //better be the same
    }

    @Test public void testSet() throws Exception {
        List<String> mList = new ArrayList<>();
        mList.add("x");
        mList.add("y");
        ImmutableList<String> l = new ArrayBackedImmutableList<String>(mList);
        Assert.assertEquals(l.size(), 2);
        Assert.assertEquals("[x, x, y, y]", l.insert(1, l).toString());
        Assert.assertEquals("[x, y, x, y]", l.insert(2, l).toString());
        Assert.assertEquals("[x, y, x, y]", l.insert(0, l).toString());
        Assert.assertEquals("[, x, y]", l.insert(0, "").toString());
        Assert.assertEquals("[x, y, ]", l.insert(2, "").toString());

        // remember, the result of these insertes was never assigned.
        Assert.assertEquals(l.size(), 2);
    }

    @Test public void testSublist() throws Exception {
        List<String> mList = new ArrayList<>();
        mList.add("x");
        mList.add("y");
        mList.add("z");
        mList.add("w");
        ImmutableList<String> l = new ArrayBackedImmutableList<String>(mList);
        Assert.assertEquals("[]", l.subList(0, 0).toString());
        Assert.assertEquals("[x]", l.subList(0, 1).toString());
        Assert.assertEquals("[x, y]", l.subList(0, 2).toString());
        Assert.assertEquals("[x, y, z]", l.subList(0, 3).toString());
        Assert.assertEquals("[x, y, z, w]", l.subList(0, 4).toString());
        Assert.assertEquals("[y, z, w]", l.subList(1, 3).toString());
        Assert.assertEquals("[y, z]", l.subList(1, 2).toString());
        Assert.assertEquals("[y, z]", l.subList(1, 2).toString());
        Assert.assertEquals("[y]", l.subList(1, 1).toString());
        Assert.assertEquals("[z]", l.subList(2, 1).toString());

    }

    @Test public void testHead() throws Exception {
        List<String> mList = new ArrayList<>();
        mList.add("x");
        mList.add("y");
        mList.add("z");
        mList.add("w");
        ImmutableList<String> l = new ArrayBackedImmutableList<String>(mList);
        Assert.assertEquals("[x]", l.head(1).toString());
        Assert.assertEquals("[]", l.head(0).toString());
        Assert.assertEquals("[x, y, z]", l.head(3).toString());
    }

    @Test public void testFirst() throws Exception {
        List<String> mList = new ArrayList<>();
        mList.add("x");
        mList.add("y");
        mList.add("z");
        mList.add("w");
        ImmutableList<String> l = new ArrayBackedImmutableList<String>(mList);
        Assert.assertEquals("x", l.first());
        Assert.assertEquals("y", l.tail(1).first());
        Assert.assertEquals("z", l.subList(2, 1).first());
    }
}
