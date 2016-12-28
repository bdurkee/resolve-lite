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
        heap.moveToCenter();
        heap.moveToEnd();
        heap.lengthen(x);
        this.fullyOrdered = false;

        while (true) {
            if (heap.atCenter()) {
                fullyOrdered = true;
                break;
            }

            int subsectNum = heap.hopIn();

            //if subsectNum < 1 do exit
            //SO... Think of it this way, our cursor is at the end of the spiral,
            //so everytime we hop in, we have roots of subsectors left to process (towards the left...)
            if (subsectNum < 1) break;
            //fixPosition();
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
        //fixPosition();
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
        T top, left, right, temp;
        int recurseOffset = 2;
        top = s.getLabel();
        if (!s.atEdge()) {
            s.hopOut();
            left = s.getLabel();
            if (gtr.test(left, top)) {

                //////////// shorthand for left :=: top;
                s.putLabel(top);
                s.hopIn();
                s.putLabel(left);
                s.hopOut();
                ////////////
                recurseOffset = 0;
            }

            if (!s.atEnd()) {
                s.spiralOut();
                right = s.getLabel();
                if (gtr.test(right, top)) {

                    //////////// shorthand for left :=: top;
                    s.putLabel(top);
                    s.hopIn();
                    s.putLabel(right);
                    s.hopOut();
                    s.spiralOut();
                    ////////////
                    recurseOffset = 1;
                }
            }
        }
    }



/*  Operation Fix_Pos(updates P : Heap_Fac.Spiral_Pos);
        requires ∀ q : Sp_Loc(2),
            (RP(k)(q) = P.Curr_Loc ⟹ q Domin_Ord_Sect P);
        ensures (P Is_Relabeling_of #P) ∧
            ( ¬r In_Sect_of P.Curr_Loc ⟹ P.Lab(r) = #P.Lab(r));
    Recursive Procedure
        decreasing SCD(P.Trmnl_Loc) - SCD(P.Curr_Loc);
        Var Top, Left, Right : Entry;
        Var recurseOffset := 2;
        Swap_Label(P, Top);

        If not At_Edge(P) then
            Hop_Out(P)
            Swap_Label(P, Left);
            If Is_Gtr(Left, Top) then
                Left :=: Top;
                recurseOffset := 0;
            end;
            Swap_Label(P, Left);

            If not At_End(P) then
                Spiral_Out(P);
                Swap_Label(P, Right);
                If Is_Gtr(Right, Top) then
                    Right :=: Top;
                    recurseOffset := 1;
                end;
                Swap_Label(P, Right)
            end;
            Hop_In(P);
        end;
        Swap_Label(P, Top);

        If recurseOffset /= 2 then
            Spiral_Out(P);
            If recurseOffset = 1 then Spiral_Out(P); end;
            Fix_Pos(P);
        end;
    end Fix_Pos;

    Operation Compare_Edge_Labels(..)
     */

    @Override
    public String toString() {
        return heap.toString();
    }






}
