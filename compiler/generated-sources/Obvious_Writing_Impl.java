/**
 * Generated from Obvious_Writing_Impl.resolve by RESOLVE version 0.0.1.
 * This file should not be modified.
 */
import java.lang.reflect.*;

public class Obvious_Writing_Impl
    implements
        Writing_Capability, Bdd_Stack_Template, InvocationHandler {
    RType E;
    RType Max_Depth;
    RType Write_Entry;
    Bdd_Stack_Template delegate;

    class Junk implements RType {
        Junk_Rep rep;
        Junk() {
            rep = new Junk_Rep(this);
            rep.initialize(this);
        }

        @Override public Object getRep() {
            return rep;
        }

        @Override public void setRep(Object o) {
            rep = (Junk_Rep)o;
        }

        @Override public RType initialValue() {
            return new Junk();
        }

        @Override public String toString() {
            return rep.toString();
        }
    }
    class Junk_Rep {
        Junk_Rep(Junk e) {
        }

        private void initialize(Junk S) {
        }

        @Override public String toString() {
            return "";
        }
    }

    public Obvious_Writing_Impl(RType E, RType Max_Depth, RType Write_Entry) {
        this(E, Max_Depth, Write_Entry, null);
    }

    /**
     * This constructor should only get invoked in the case of enhancements; as
     * the last parameter 'delegate' takes an instance of the base concept.
     * <p>
     * The only reason it's here is simplicity. If the compiler is correct,
     * this will never get invoked from the wrong place.</p>
     */
    public Obvious_Writing_Impl(RType E, 
                                RType Max_Depth, 
                                RType Write_Entry, 
                                Bdd_Stack_Template delegate) {
        this.E = E;
        this.Max_Depth = Max_Depth;
        this.Write_Entry = Write_Entry;
        this.delegate = delegate;
    }
    @Override public void Write(RType S) {
        RType Next = this.initE();
        while (((Boolean_Impl.Boolean)(((Integer_Template)Standard_Integers.INSTANCE).Less_Or_Equal(((Integer_Template)Standard_Integers.INSTANCE).initInteger(1), this.Depth(S)))).rep.val) {
            this.Pop(Next, S);
            ((OperationParameter)Write_Entry).op(Next);
        }
    }

    @Override public RType initJunk() {
        return new Junk();
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
    @Override public RType initStack() {
        return delegate.initStack();
    }
    @Override public void Push(RType e, RType S) {
    delegate.Push(e, S);
    }
    @Override public void Pop(RType e, RType S) {
    delegate.Pop(e, S);
    }
    @Override public RType Depth(RType S) {
        return delegate.Depth(S);
    }
    @Override public RType Rem_Capacity(RType S) {
        return delegate.Rem_Capacity(S);
    }
    @Override public void Clear(RType S) {
    delegate.Clear(S);
    }
    @Override public Object invoke(Object proxy, Method method,
            Object[] args) throws Throwable {
        switch (method.getName()) {
                case "Write": return method.invoke(this, args);
                case "getE": return method.invoke(this, args);
                case "initE": return method.invoke(this, args);
                case "getMax_Depth": return method.invoke(this, args);
                default: return method.invoke(delegate, args);
                }
    }

    public static Bdd_Stack_Template createProxy(RType E, 
                                                 RType Max_Depth, 
                                                 RType Write_Entry, 
                                                 Bdd_Stack_Template toWrap) {
        Obvious_Writing_Impl eObj =
                new Obvious_Writing_Impl(E, Max_Depth, Write_Entry, toWrap);
        Class[] toWrapInterfaces = toWrap.getClass().getInterfaces();
        Class[] thisInterfaces = new Class[toWrapInterfaces.length+1];
        Class[] tmpInterfaces = eObj.getClass().getInterfaces();
        thisInterfaces[0] = tmpInterfaces[0];
        System.arraycopy(toWrapInterfaces, 0, thisInterfaces, 1,
                toWrapInterfaces.length);
        return (Bdd_Stack_Template)(Proxy.newProxyInstance(Bdd_Stack_Template
                .class.getClassLoader(), thisInterfaces, eObj));
    }
}