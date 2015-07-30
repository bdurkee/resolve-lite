/**
 * Generated from Bdd_Stack_Template.resolve by RESOLVE version 0.0.1.
 * This file should not be modified.
 */
import java.lang.reflect.*;

public interface Bdd_Stack_Template {
    interface Stack extends RType { }
    public RType initStack();
    public void Push(RType e, RType S);
    public void Pop(RType e, RType S);
    public RType Depth(RType S);
    public RType Rem_Capacity(RType S);
    public void Clear(RType S);
    public RType getE();
    public RType getMax_Depth();
}