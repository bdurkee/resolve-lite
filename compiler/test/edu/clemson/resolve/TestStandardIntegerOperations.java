package edu.clemson.resolve;

import org.junit.Assert;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestStandardIntegerOperations extends BaseTest {

    @Test public void testIncrAndDecr() throws Exception {
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

    @Test public void testSumAndMinus() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers;" +
                        "Operation Main(); " +
                        "   Procedure Var i : Std_Integer_Fac :: Integer;" +
                        "   Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i+1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i+1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i-1; Std_Integer_Fac :: Write_Line(i);" +
                        "   i:=i-1; Std_Integer_Fac :: Write_Line(i);" +
                        "end Main;" +
                        "end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("0\n1\n2\n1\n0\n", found);
    }

    @Test public void testRelationals() throws Exception {
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

    @Test public void testRelationalEquals() throws Exception {
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

    @Test public void testProduct() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers;" +
                        "Operation Main(); Procedure" +
                        "   Std_Integer_Fac :: Write_Line(3*2*8);" +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("48\n", found);
    }

    @Test public void testDivide() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers;" +
                        "Operation Main(); Procedure" +
                        "   Std_Integer_Fac :: Write_Line((8 / 2) / 2);" +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("2\n", found);
    }

    @Test public void testNegate() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers;" +
                        "Operation Main(); Procedure" +
                        "   Var x,y : Std_Integer_Fac :: Integer;" +
                        "   y:=5; -1;" +
                        "   Std_Integer_Fac :: Write_Line(x); x:=-1;" +
                        "   Std_Integer_Fac :: Write_Line(x);" +
                        "   Std_Integer_Fac :: Write_Line(-0);" +
                        "   Std_Integer_Fac :: Write_Line(y); -y;" +
                        "   Std_Integer_Fac :: Write_Line(y);" +
                        "   Std_Integer_Fac :: Write_Line(-y);" +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("0\n-1\n0\n5\n5\n-5\n", found);
    }
}
