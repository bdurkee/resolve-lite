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

        String found = execCode("T.resolve", facility, "T", "", false);
        Assert.assertEquals("00 \n1\n", found);
    }

    @Test public void testSimpleCall() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Characters;" +
                        "Operation Foo(alters e : Std_Integer_Fac :: Integer); " +
                        "   Procedure e:=e+1; end Foo;" +
                        "Operation Main(); " +
                        "   Procedure Var x: Std_Integer_Fac :: Integer;" +
                        "   Std_Integer_Fac :: Write(x); Foo(x); " +
                        "   Std_Integer_Fac :: Write(x); " +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();

        String found = execCode("T.resolve", facility, "T", "", false);
        Assert.assertEquals("01\n", found);
    }

    @Test public void testCallWithReturn() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Operation Prefix_Dog_with (alters prefix : Std_Char_Str_Fac :: Char_Str) :" +
                        "   Std_Char_Str_Fac :: Char_Str; " +
                        "   Procedure Prefix_Dog_with:=prefix++\"Dog\"; end Foo;" +
                        "Operation Main(); " +
                        "   Procedure Var x: Std_Char_Str_Fac :: Char_Str;" +
                        "   x:=\"cat\"; x:=Prefix_Dog_with(x); " +
                        "   Std_Char_Str_Fac :: Write_Line(x); " +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();

        String found = execCode("T.resolve", facility, "T", "", false);
        
        Assert.assertEquals("catDog\n", found);
    }


}
