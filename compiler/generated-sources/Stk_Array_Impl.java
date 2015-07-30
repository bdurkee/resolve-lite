/**
 * Generated from Stk_Array_Impl.resolve by RESOLVE version 0.0.1.
 * This file should not be modified.
 */
import java.lang.reflect.*;

public class Stk_Array_Impl implements Bdd_Stack_Template {
    RType E;
    RType Max_Depth;
    Bdd_Stack_Template delegate;
    Bdd_Ceramic_Array_Template Ceramic_Arr_Fac;
    class Stack implements RType {
        Stack_Rep rep;
        Stack() {
            rep = new Stack_Rep(this);
            rep.initialize(this);
        }

        @Override public Object getRep() {
            return rep;
        }

        @Override public void setRep(Object o) {
            rep = (Stack_Rep)o;
        }

        @Override public RType initialValue() {
            return new Stack();
        }

        @Override public String toString() {
            return rep.toString();
        }
    }
    class Stack_Rep {
        RType Contents;
        RType Top;
        Stack_Rep(Stack e) {
            this.Contents = ((Bdd_Ceramic_Array_Template)Ceramic_Arr_Fac).initCeramic_Array();
            this.Top = ((Integer_Template)Standard_Integers.INSTANCE).initInteger();
        }

        private void initialize(Stack S) {
            RESOLVEBase.assign((((Stk_Array_Impl.Stack)S)).rep.Top, ((Integer_Template)Standard_Integers.INSTANCE).initInteger(0));
        }

        @Override public String toString() {
            return Contents.toString()+Top.toString();
        }
    }
    public RType initStack() {
        return new Stack();
    }

    public RType getStack() {
        return initStack();
    }
    public Stk_Array_Impl(RType E, RType Max_Depth) {
        this(E, Max_Depth, null);
    }

    /**
     * This constructor should only get invoked in the case of enhancements; as
     * the last parameter 'delegate' takes an instance of the base concept.
     * <p>
     * The only reason it's here is simplicity. If the compiler is correct,
     * this will never get invoked from the wrong place.</p>
     */
    public Stk_Array_Impl(RType E, 
                          RType Max_Depth, 
                          Bdd_Stack_Template delegate) {
        this.E = E;
        this.Max_Depth = Max_Depth;
        this.Ceramic_Arr_Fac = new Ceramic_Array_Impl(this.getE(),
         ((Integer_Template)Standard_Integers.INSTANCE).initInteger(1),
         getMax_Depth());
        this.delegate = delegate;
    }
    @Override public void Push(RType e, RType S) {
        RESOLVEBase.assign((((Stk_Array_Impl.Stack)S)).rep.Top, ((Integer_Template)Standard_Integers.INSTANCE).Sum((((Stk_Array_Impl.Stack)S)).rep.Top, ((Integer_Template)Standard_Integers.INSTANCE).initInteger(1)));
        ((Bdd_Ceramic_Array_Template)Ceramic_Arr_Fac).Swap_Element((((Stk_Array_Impl.Stack)S)).rep.Contents, e, (((Stk_Array_Impl.Stack)S)).rep.Top);
    }

    @Override public void Pop(RType e, RType S) {
        ((Bdd_Ceramic_Array_Template)Ceramic_Arr_Fac).Swap_Element((((Stk_Array_Impl.Stack)S)).rep.Contents, e, (((Stk_Array_Impl.Stack)S)).rep.Top);
        RESOLVEBase.assign((((Stk_Array_Impl.Stack)S)).rep.Top, ((Integer_Template)Standard_Integers.INSTANCE).Difference((((Stk_Array_Impl.Stack)S)).rep.Top, ((Integer_Template)Standard_Integers.INSTANCE).initInteger(1)));
    }

    @Override public RType Depth(RType S) {
        RType Depth = (((Stk_Array_Impl.Stack)S)).rep.Top;
        RESOLVEBase.assign(Depth, (((Stk_Array_Impl.Stack)S)).rep.Top);
        return Depth;
    }

    @Override public RType Rem_Capacity(RType S) {
        RType Rem_Capacity = ((Integer_Template)Standard_Integers.INSTANCE).Difference(Max_Depth, (((Stk_Array_Impl.Stack)S)).rep.Top);
        RESOLVEBase.assign(Rem_Capacity, ((Integer_Template)Standard_Integers.INSTANCE).Difference(Max_Depth, (((Stk_Array_Impl.Stack)S)).rep.Top));
        return Rem_Capacity;
    }

    @Override public void Clear(RType S) {
        RESOLVEBase.assign((((Stk_Array_Impl.Stack)S)).rep.Top, ((Integer_Template)Standard_Integers.INSTANCE).initInteger(0));
    }

    @Override public RType getE() {
        return E;
    }

    public RType initE() {
        return E;
    }

    @Override public RType getMax_Depth() {
        return Max_Depth;
    }
}