package edu.clemson.resolve.codegen;

import edu.clemson.resolve.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestCalls extends BaseTest {

    @Test public void testArglessOpCall() throws Exception {
        mkdir(tmpdir);
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Characters;" +
                "Operation Foo(); Procedure \n end Foo;" +
                "Operation Main(); Procedure " +
                    "Var x,y : Std_Integer_Fac :: Integer;" +
                    "Std_Integer_Fac :: Write(x);" +
                    "Std_Integer_Fac :: Write(y);" +
                    "y:=x+1;" +
                    "Std_Character_Fac :: Write_Line(' ');" +
                    "Std_Integer_Fac :: Write_Line(y);" +
                    "Foo(); " +
                "end Main;" +
                "end T;");
        String facility = facilityST.render();

        String input = "";
        String found = execCode("T.resolve", facility, "T", input, false);
        Assert.assertEquals("00 \n1\n", found);
    }


}
