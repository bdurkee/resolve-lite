package edu.clemson.resolve.spiral_heap_test;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class HeapBacked<T> implements Prioritizer<T> {

    private final BiPredicate<T, T> gtr;
    private final Spiral<T> heap = new LinkedListBackedSpiral<T>(2);
    private boolean isAccepting, fullyOrdered = false;

    public HeapBacked(BiPredicate<T, T> gtr) {
        this.gtr = gtr;
    }

    @Override
    public void addEntry(T x) {
        heap.moveToEnd();
        heap.lengthen(x);
        this.fullyOrdered = false;

        /*while (true) {
            if (heap.atCenter()) { fullyOrdered = true; break; }
            int subsectNum = heap.hopIn();
            //if (subsectNum < 1) break;
            fixPosition(heap);
        }*/
        while (!heap.atCenter()) {
            int subsectNum = heap.hopIn();
            fixPosition(heap);
        }
    }

    @Override
    public void fullyOrder(List<T> entries) {
        for (T t : entries) {
            heap.lengthen(t);
        }
        heap.moveToEnd();
        heap.hopIn();
        while (true) {
            fixPosition(heap);
            if (heap.atCenter()) break;
            heap.spiralIn();   //TODO: This needs to be spiral_in I think..
        }
    }

    @Override
    public boolean changeMode() {
        return isAccepting;
    }

    @Override
    public T removeSmallest() {
        T result;
        T x = heap.shorten();
        if (heap.atCenter()) return x;
        heap.moveToCenter();
        result = heap.swapLabel(x);
        fixPosition(heap);
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

    //Type Entry_Keeper is Record
    //      Heap : Heap_Spiral_Fac :: Spiral_Pos;
    //      Accpt_Flag, Fully_Ord_Flag : Boolean;
    //  end;
    //  conventions (K.Fully_Ord_Flag := not K.Accpt_Flag);
    //  correspondence conc.K.Accepting = K.Accpt_Flag ∧
    //      conc.K.Entry_Tally =
    //          K.Heap.Lab[Inward_Loc(K.Heap.Trmnl_Loc) ⋆ 1]

    private void fixPosition(Spiral<T> s) {
        T top, small_sect_pos;
        int offset = 0;

        if (!s.atEdge()) {
            top = s.swapLabel(null);
            s.hopOut();

            if (!s.atEnd()) moveToGtrPos(s);
            small_sect_pos = s.swapLabel(null);

            if (gtr.test(small_sect_pos, top)) {
                //basically: top :=: small_sect_pos
                T temp = top;
                top = small_sect_pos;
                small_sect_pos = temp;
            }
            s.swapLabel(small_sect_pos);
            fixPosition(s);
            s.hopIn();
            s.swapLabel(top);
        }
    }

    private void moveToGtrPos(Spiral<T> s) {
        T left, right;
        boolean l_side;

        left = s.swapLabel(null);
        s.spiralOut();
        right = s.swapLabel(null);

        l_side = gtr.test(left, right);

        s.swapLabel(right);
        s.spiralIn();
        s.swapLabel(left);
        if (!l_side) s.spiralOut();
    }

    @Override
    public String toString() {
        return heap.toString();
    }
}
