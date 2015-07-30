/**
 * Generated from Obvious_Reading_Impl.resolve by RESOLVE version 0.0.1.
 * This file should not be modified.
 */
import java.lang.reflect.*;

public class Obvious_Reading_Impl
    implements
        Reading_Capability, Bdd_Stack_Template, InvocationHandler {
    RType E;
    RType Max_Depth;
    RType Read_Element;
    Bdd_Stack_Template delegate;

    public Obvious_Reading_Impl(RType E, RType Max_Depth, RType Read_Element) {
        this(E, Max_Depth, Read_Element, null);
    }

    /**
     * This constructor should only get invoked in the case of enhancements; as
     * the last parameter 'delegate' takes an instance of the base concept.
     * <p>
     * The only reason it's here is simplicity. If the compiler is correct,
     * this will never get invoked from the wrong place.</p>
     */
    public Obvious_Reading_Impl(RType E, 
                                RType Max_Depth, 
                                RType Read_Element, 
                                Bdd_Stack_Template delegate) {
        this.E = E;
        this.Max_Depth = Max_Depth;
        this.Read_Element = Read_Element;
        this.delegate = delegate;
    }
    @Override public void Read_upto(RType S, RType Count) {
        RType Next = this.initE();
        this.Clear(S);
        while (((Boolean_Impl.Boolean)(((Integer_Template)Standard_Integers.INSTANCE).Less(this.Depth(S), Count))).rep.val) {
            ((OperationParameter)Read_Element).op(Next);
            this.Push(Next, S);
        }
    }
    @Override public void Read(RType S) {
        this.Read_upto(S, Max_Depth);
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
                case "Read_upto": return method.invoke(this, args);
                case "Read": return method.invoke(this, args);
                case "getE": return method.invoke(this, args);
                case "initE": return method.invoke(this, args);
                case "getMax_Depth": return method.invoke(this, args);
                default: return method.invoke(delegate, args);
                }
    }

    public static Bdd_Stack_Template createProxy(RType E, 
                                                 RType Max_Depth, 
                                                 RType Read_Element, 
                                                 Bdd_Stack_Template toWrap) {
        Obvious_Reading_Impl eObj =
                new Obvious_Reading_Impl(E, Max_Depth, Read_Element, toWrap);
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