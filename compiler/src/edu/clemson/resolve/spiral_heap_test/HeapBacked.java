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

    /*
    Operation Fix_Pos(updates P : Heap_Fac.Spiral_Pos);
        requires ∀ q : Sp_Loc(2),
            (RP(k)(q) = P.Curr_Loc ⟹ q Domin_Ord_Sect P);
        ensures (P Is_Relabeling_of #P) ∧
            ( ¬r In_Sect_of P.Curr_Loc ⟹ P.Lab(r) = #P.Lab(r));
    Recursive Procedure
        decreasing SCD(P.Trmnl_Loc) - SCD(P.Curr_Loc);
        Var Top, Left, Right : Entry;
        Var recurseOffset := 2;

        If not At_End(P) then
            Swap_Label(P, Top);
            Spiral_Out(P)
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

                Hop_Out(P);
                Swap_Label(P, Top);
            end;
        end;

        If



    end Fix_Pos;

     */




    //  T top = s.swapLabel(null)
    //int offsetNum = Negate(1);
    //if not s.atEnd()
    //  s.spiralOut();
    //  left = s.swapLabel(temp);
    //  if gtr.test(left, top)
    //      left :=: top;
    //      offsetNum = s.hopIn()
    //      s.swapLabel(top);
    //      s.spiralOut();
    //  end;
    //  s.swapLabel(left)

    //if not s.atEnd()
    //  s.spiralOut();
    //  left = s.swapLabel(temp);
    //  if gtr.test(left, top) top :=: left;
    //  s.swapLabel(left)


    private void fixPosition(Spiral<T> s) {

        //T largest
        //  top = s.swapLabel(temp);

        //if not s.atEnd()
        //  s.spiralOut();
        //  left = s.swapLabel(temp);
        //  if gtr.test(left, top) top :=: left;
        //  s.swapLabel(left)

        //if not s.atEnd()
        //  s.spiralOut();
        //  left = s.swapLabel(temp);
        //  if gtr.test(left, top) top :=: left;
        //  s.swapLabel(left)

        //!use offsetNum to tell which child
        //to recurse on...!

        //you want to position the current
        //cursor where the max element is...



        //T left, right, top;
        //if not at_end
        //  top = s.swapLabel(temp);
        //  s.hopOut();
        //  left = s.swapLabel(temp);

        //  if not at_end
        //      s.spiralOut();
        //      right = s.swapLabel(null);
        //  end;
        //  s.hopIn();
        //  s.swapLabel(top)
        //  if (gtr.test(left, top))
        //      s.hopOut;
        //      s.swapLabel(left)
        //  if gtr.test(right, top)
        //


        //

        //so we remove labels, recurse, then sift them? NO. Before we can
        //recurse, we need to know which sector to recurse *on/into*
        //if not at_end and gtr.test(left, top))



        //

        //
    }

    //T left, right, top;
    //if not at_end
    //  top = s.swapLabel(temp);
    //  s.hopOut();
    //  left = s.swapLabel(temp);
    //  if gtr.test(left, top) top :=: left
    //  s.swap_label(left)

    //  if not at_end
    //      s.spiralOut();
    //      right = s.swapLabel(null);
    //      if gtr.test(top, right) top :=: right;
    //      //put the right side back
    //      s.swap_label(right);
    //  else
    //      //replace the top
    //  end;

    @Override
    public String toString() {
        return heap.toString();
    }


        /*
    Operation Fix_Pos(updates P : Heap_Fac.Spiral_Pos);
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
            Hop_Out(P);
        end;
        Swap_Label(P, Top);

        If recurseOffset /= 2 then
            Spiral_Out(P);
            If recurseOffset = 1 then Spiral_Out(P); end;
            Fix_Pos(P);
        end;
    end Fix_Pos;

    Operation Compare_Edge_Labels(
     */



}
