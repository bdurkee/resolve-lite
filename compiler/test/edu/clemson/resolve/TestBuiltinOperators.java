package edu.clemson.resolve;

import org.junit.Assert;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestBuiltinOperators extends BaseTest {
    //Swap, assignment,
    //do some involving structs.
    @Test public void testAssignAndSwap() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Operation Main(); Procedure" +
                        "   Var x,y : Std_Integer_Fac :: Integer;" +
                        "   x:=3; y:=5;" +
                        "   Std_Integer_Fac :: Write_Line(x); " +
                        "   Std_Integer_Fac :: Write_Line(y); " +
                        "   x:=:y;" +
                        "   Std_Integer_Fac :: Write_Line(x); " +
                        "   Std_Integer_Fac :: Write_Line(y); " +
                        "   x:=:y;" +
                        "   Std_Integer_Fac :: Write_Line(x); " +
                        "   Std_Integer_Fac :: Write_Line(y); " +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("3\n5\n5\n3\n3\n5\n", found);
    }
}
