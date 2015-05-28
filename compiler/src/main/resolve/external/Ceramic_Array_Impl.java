/**
 * Generated from Insertion_Sorting_Impl.impl by RESOLVE version 2.22.15a.
 * This file should not be modified.
 */
import org.resolvelite.runtime.*;
import java.lang.reflect.*;

public class Insertion_Sorting_Impl implements Prioritizer_Template {
    RType T;
    RType Max_Capacity;
    RType Is_Lss_Eq;
    public RType Is_Lss_Eq(RType x, RType y) {
        return ((OperationParameter)Is_Lss_Eq).op(x, y);
    }
    Bdd_Ceramic_Array_Template Ceramic_Arr_Fac;
    class Keeper implements RType {
        Keeper_Rep rep;
        Keeper() {
            rep = new Keeper_Rep();
        }

        @Override public Object getRep() {
            return rep;
        }

        @Override public void setRep(Object o) {
            rep = (Keeper_Rep)o;
        }

        @Override public RType initialValue() {
            return new Keeper();
        }

        @Override public String toString() {
            return rep.toString();
        }
    }
    class Keeper_Rep {
        RType Seq;
        RType Accepting_Flag;
        Keeper_Rep() {
            Seq = ((Bdd_Ceramic_Array_Template)Ceramic_Arr_Fac).initCeramic_Array();
            Accepting_Flag = ((Boolean_Template)Standard_Booleans.INSTANCE).initBoolean();
        }
        @Override public String toString() {
            return Seq.toString()+Accepting_Flag.toString();
        }
    }
    public RType initKeeper() {
        return new Keeper();
    }

    public RType getKeeper() {
        return initKeeper();
    }
    public Insertion_Sorting_Impl(RType T, RType Max_Capacity, RType Is_Lss_Eq) {
        this.T = T;
        this.Max_Capacity = Max_Capacity;
        this.Is_Lss_Eq = Is_Lss_Eq;
        this.Ceramic_Arr_Fac = new Ceramic_Array_Impl(this.getT(),
                ((Integer_Template)Standard_Integers.INSTANCE).initInteger(1),
                this.getMax_Capacity());
    }
    @Override public void Add(RType e, RType K) {
    }

    @Override public RType getT() {
        return T;
    }

    public RType initT() {
        return T;
    }

    @Override public RType getMax_Capacity() {
        return Max_Capacity;
    }
}