/**
 * Generated from Alt_Rev_Stack.resolve by RESOLVE version 0.0.1.
 * This file should not be modified.
 */
import java.lang.reflect.*;

public class Alt_Rev_Stack {
    public static final Bdd_Stack_Template SF = Obvious_Writing_Impl.createProxy(((Integer_Template)Standard_Integers.INSTANCE).initInteger(),
     ((Integer_Template)Standard_Integers.INSTANCE).initInteger(4),
     new OperationParameter() {
            @Override public RType op(RType... e) {
                ((Integer_Template)Standard_Integers.INSTANCE).Write(e[0]);
                                return null;
            }

            @Override public Object getRep() {
                throw new UnsupportedOperationException("getRep() shouldn't be "
                    + "getting called from: " + this.getClass());
            }

            @Override public void setRep(Object setRep) {
                throw new UnsupportedOperationException("setRep() shouldn't be "
                    + "getting called from: " + this.getClass());
            }

            @Override public RType initialValue() {
                throw new UnsupportedOperationException("initialValue() shouldn't"
                       + " be getting called from: " + this.getClass());
            }
        },
     Obvious_Reading_Impl.createProxy(((Integer_Template)Standard_Integers.INSTANCE).initInteger(),
     ((Integer_Template)Standard_Integers.INSTANCE).initInteger(4),
     new OperationParameter() {
            @Override public RType op(RType... e) {
                ((Integer_Template)Standard_Integers.INSTANCE).Read(e[0]);
                                return null;
            }

            @Override public Object getRep() {
                throw new UnsupportedOperationException("getRep() shouldn't be "
                    + "getting called from: " + this.getClass());
            }

            @Override public void setRep(Object setRep) {
                throw new UnsupportedOperationException("setRep() shouldn't be "
                    + "getting called from: " + this.getClass());
            }

            @Override public RType initialValue() {
                throw new UnsupportedOperationException("initialValue() shouldn't"
                       + " be getting called from: " + this.getClass());
            }
        },
     new Stk_Array_Impl(((Integer_Template)Standard_Integers.INSTANCE).initInteger(),
     ((Integer_Template)Standard_Integers.INSTANCE).initInteger(4))));
    public static void Main() {
        RType S = ((Bdd_Stack_Template)SF).initStack();
        RType Next2 = ((Bdd_Stack_Template)SF).initJunk();

        ((Bdd_Stack_Template)SF).Read(S);
        ((Char_Str_Template)Standard_Char_Strings.INSTANCE).Write_Line(((Char_Str_Template)Standard_Char_Strings.INSTANCE).initChar_Str("reversed order:"));
        ((Bdd_Stack_Template)SF).Write(S);
    }
    public static void main(String[] args) {
        Main();
    }
}