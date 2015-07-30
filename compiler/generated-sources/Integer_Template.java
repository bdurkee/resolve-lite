/**
 * Generated from Integer_Template.resolve by RESOLVE version 0.0.1.
 * This file should not be modified.
 */
import java.lang.reflect.*;

public interface Integer_Template {
    interface Integer extends RType { }
    public RType initInteger(int... e);
    public void Increment(RType i);
    public void Decrement(RType i);
    public RType Sum(RType i, RType j);
    public RType Difference(RType i, RType j);
    public RType Less_Or_Equal(RType i, RType j);
    public RType Less(RType i, RType j);
    public RType Greater(RType i, RType j);
    public RType Greater_Or_Equal(RType i, RType j);
    public RType Is_Zero(RType i);
    public RType Is_Not_Zero(RType i);
    public RType Product(RType i, RType j);
    public void Divide(RType i, RType j, RType q);
    public RType Negate(RType i);
    public RType Are_Equal(RType i, RType j);
    public RType Are_Not_Equal(RType i, RType j);
    public void Read(RType i);
    public void Write(RType i);
    public void Write_Line(RType i);
}