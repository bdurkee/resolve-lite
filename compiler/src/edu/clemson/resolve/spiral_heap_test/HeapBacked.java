package edu.clemson.resolve.spiral_heap_test;

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

        while (true) {
            if (heap.atCenter()) { fullyOrdered = true; break; }
            int subsectNum = heap.hopIn();
            //if (subsectNum < 1) break;
            fixPosition(heap);
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
        T left, right;
        boolean leftside = false;
        if (!s.atEdge()) {
            s.hopOut();
            if (!s.atEnd()) {
                left = s.swapLabel(null);
                s.spiralOut();
                right = s.swapLabel(null);
                leftside = gtr.test(left, right);
                s.swapLabel(right);
                s.spiralIn();
                s.swapLabel(left);
                if (!leftside) s.spiralOut();
            }
            int offset = 0;
            T curr = s.swapLabel(null);
            offset = s.hopIn();
            T top = s.swapLabel(null);

            boolean lss = gtr.test(curr, top);
            if (gtr.test(curr, top)) {
                T temp = top;
                top = curr;
                curr = temp;

                s.swapLabel(top);
                s.hopOut();
                if (offset == 1) s.spiralOut();
                s.swapLabel(curr);
                fixPosition(s);
                s.hopIn();
            }
            else { //no change case...
                s.swapLabel(top);
                s.hopOut();
                s.swapLabel(curr);
                s.hopIn();
            }
        }
    }



    /*
    Operation Fix_Pos(updates P : Heap_Fac.Spiral_Pos);
    requires ∀ q : Sp_Loc(2),
        (RP(k)(q) = P.Curr_Loc ⟹ q Domin_Ord_Sect P);
    ensures (P Is_Relabeling_of #P) ∧
        (∀ r : Sp_Loc(2),
            ¬r In_Sect_of P.Curr_Loc ⟹ P.Lab(r) = #P.Lab(r));
    Recursive Procedure
        Var Left, Right : Entry;

        If not At_Edge(P) then
            Hop_Out(P);
            If not At_End(P) then
                Move_to_Minimum(P);
            end;

            if (gtr.test(curr, top)) {
                T temp = top;
                top = curr;
                curr = temp;

                s.swapLabel(top);
                s.hopOut();
                if (offset == 1) s.spiralOut();
                s.swapLabel(curr);
                fixPosition(s);
                s.hopIn();
            }
            else { //no change case...
                s.swapLabel(top);
                s.hopOut();
                s.swapLabel(curr);
                s.hopIn();
            }
        }
        end Fix_Pos;

        //Updates the position of the cursor to the minimum subsector..
        Operation Move_to_Minimum(updates P : Spiral_Pos);
            Procedure
            Var Left, Right : Entry;
            Swap_Label(P, Left);
            Spiral_Out(P);
            Swap_Label(P, Right);
            L_Side := Is_Gtr(Left, Right);
            Swap_Label(P, Right);
            Spiral_In(P);
            Swap_Label(P, Left);
            If not L_Side then Spiral_Out(P); end;
        end Find_Minimum;
    */

    @Override
    public String toString() {
        return heap.toString();
    }






}
