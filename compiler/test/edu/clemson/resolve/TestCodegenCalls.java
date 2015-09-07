package edu.clemson.resolve;

import edu.clemson.resolve.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestCodegenCalls extends BaseTest {

    @Test public void testArglessOpCall() throws Exception {
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

        String found = execCode("T.resolve", facility, "T", false);
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

        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("01\n", found);
    }

    @Test public void testCallWithReturn() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Operation Prefix_Dog_with (alters prefix : Std_Char_Str_Fac :: Char_Str) :" +
                        "   Std_Char_Str_Fac :: Char_Str; " +
                        "   Procedure Prefix_Dog_with:=prefix++\"Dog\"; end Prefix_Dog_with;" +
                        "Operation Main(); " +
                        "   Procedure Var x: Std_Char_Str_Fac :: Char_Str;" +
                        "   x:=\"cat\"; x:=Prefix_Dog_with(x); " +
                        "   Std_Char_Str_Fac :: Write_Line(x); " +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("catDog\n", found);
    }

    @Test public void testSugardDecrIncrCalls() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers;" +
                        "Operation Main(); " +
                        "   Procedure Var i : Std_Integer_Fac :: Integer;" +
                        "   i++; " +
                        "   Std_Integer_Fac :: Write_Line(i); " +
                        "   i--;" +
                        "   Std_Integer_Fac :: Write_Line(i); " +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("1\n0\n", found);
    }

    @Test public void testSugardArithmethticCalls() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers;" +
                        "Operation Main(); " +
                        "   Procedure Var i : Std_Integer_Fac :: Integer;" +
                        "   Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i+1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i+1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i+1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i+1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i+1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i-1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i-1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i-1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i-1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i-1; Std_Integer_Fac :: Write_Line(i);" +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("0\n1\n2\n3\n4\n5\n4\n3\n2\n1\n0\n", found);
    }

    @Test public void testRelationalCalls() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Booleans;" +
                        "Operation Main(); " +
                        "   Procedure Var i,j : Std_Integer_Fac :: Integer;" +
                        "   i++; " +
                        "   Std_Boolean_Fac :: Write_Line(i\\<j); j++;" +
                        "   Std_Boolean_Fac :: Write_Line(i\\<j); j++;" +
                        "   Std_Boolean_Fac :: Write_Line(i\\<j);" +
                        "   Std_Boolean_Fac :: Write_Line(i\\<=j); j--;" +
                        "   Std_Boolean_Fac :: Write_Line(i\\<=j); j--;" +
                        "   Std_Boolean_Fac :: Write_Line(i>j);" +
                        "   Std_Boolean_Fac :: Write_Line(i>=j); i--;" +
                        "   Std_Boolean_Fac :: Write_Line(i>j); " +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("false\nfalse\ntrue\ntrue\ntrue\ntrue\ntrue\nfalse\n", found);
    }

    @Test public void testRelationalEqualCalls() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Booleans;" +
                        "Operation Main(); " +
                        "   Procedure Var i,j : Std_Integer_Fac :: Integer;" +
                        "   i++; " +
                        "   Std_Boolean_Fac :: Write_Line(i/=j); j++;" +
                        "   Std_Boolean_Fac :: Write_Line(i=j);" +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("true\ntrue\n", found);
    }

    @Test public void testWrappedArithmetic() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Booleans;" +
                        "Operation Main(); " +
                        "   Procedure Var i,j : Std_Integer_Fac :: Integer;" +
                        "   i++; " +
                        "   Std_Boolean_Fac :: Write_Line(i/=j); j++;" +
                        "   Std_Boolean_Fac :: Write_Line(i=j);" +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("true\ntrue\n", found);
    }

}
