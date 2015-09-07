package edu.clemson.resolve;

import org.junit.Assert;
import org.junit.Test;
import org.stringtemplate.v4.ST;

/**
 * Tests both sugared and unsugared calls to the standard template operations
 * provided by {@code Boolean_Template}.
 */
public class TestStandardBooleanCalls extends BaseTest {

    @Test public void testTrueFalseCalls() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Booleans;" +
                        "Operation Main(); " +
                        "   Procedure Var b : Std_Boolean_Fac :: Boolean;" +
                        "   b:=Std_Boolean_Fac :: True(); " +
                        "   Std_Boolean_Fac :: Write_Line(b); " +
                        "   b:=Std_Boolean_Fac :: False(); " +
                        "   Std_Boolean_Fac :: Write_Line(b); " +
                        "   b:=true;Std_Boolean_Fac :: Write_Line(b);b:=false;" +
                        "   Std_Boolean_Fac :: Write_Line(b);" +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("true\nfalse\ntrue\nfalse\n", found);
    }

    @Test public void testLogicalConnectiveCalls() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Booleans;" +
                        "Operation Main(); " +
                        "   Procedure Var a,b,c,d : Std_Boolean_Fac :: Boolean;" +
                        "   a:=true;b:=false; " +
                        "   c:=Std_Boolean_Fac :: And(a, b); " +
                        "   d:=Std_Boolean_Fac :: Or(a, b); " +
                        "   Std_Boolean_Fac :: Write_Line(c);"+
                        "   Std_Boolean_Fac :: Write_Line(d);"+
                        "   Std_Boolean_Fac :: Write_Line((a and b) or c);"+
                        "   Std_Boolean_Fac :: Write_Line((a or b) and c);"+
                        "   Std_Boolean_Fac :: Write_Line((a or b) or c);"+
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("false\ntrue\nfalse\nfalse\ntrue\n", found);
    }
}
