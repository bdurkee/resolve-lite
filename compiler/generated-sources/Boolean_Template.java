/**
 * Generated from Boolean_Template.resolve by RESOLVE version 0.0.1.
 * This file should not be modified.
 */
import java.lang.reflect.*;

public interface Boolean_Template {
    interface Boolean extends RType { }
    public RType initBoolean(boolean... e);
    public RType True();
    public RType False();
    public RType And(RType b1, RType b2);
    public RType Or(RType b1, RType b2);
    public RType Not(RType b);
    public void Read(RType b);
    public void Write(RType b);
    public void Write_Line(RType b);
}