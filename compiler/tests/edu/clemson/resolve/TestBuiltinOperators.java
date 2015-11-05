package edu.clemson.resolve;

import org.junit.Assert;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestBuiltinOperators extends BaseTest {

    /*@Test public void testAssignAndSwap() throws Exception {
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

    @Test public void testAssignAndSwapWithStructs() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Type Foo = Record" +
                        "   x,y : Std_Integer_Fac :: Integer;" +
                        "   end;" +
                        "Operation Main(); Procedure" +
                        "   Var F,X : Foo;" +
                        "   F.x:=32; X.x:=88;" +
                        "   Std_Integer_Fac :: Write_Line(F.x); " +
                        "   Std_Integer_Fac :: Write_Line(X.x); " +
                        "   F:=:X;" +
                        "   Std_Integer_Fac :: Write_Line(F.x); " +
                        "   Std_Integer_Fac :: Write_Line(X.x); " +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("32\n88\n88\n32\n", found);
    }

    @Test public void testAssignAndSwapWithStructs2() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Type Foo = Record" +
                        "   i,j : Std_Integer_Fac :: Integer;" +
                        "   end;" +
                        "Operation Main(); Procedure" +
                        "   Var F,X : Foo; Var I : Std_Integer_Fac :: Integer;" +
                        "   F.i:=32; X.i:=88;" +
                        "   I:=F.i; " +
                        "   Std_Integer_Fac :: Write_Line(I); " +
                        "   Std_Integer_Fac :: Write_Line(F.i); " +
                        "   X:=:F;" +
                        "   Std_Integer_Fac :: Write_Line(I); " +
                        "   Std_Integer_Fac :: Write_Line(F.i); " +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("32\n32\n32\n88\n", found);
    }

    @Test public void testStructFieldSwap() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Type Foo = Record" +
                        "   i,j : Std_Integer_Fac :: Integer;" +
                        "   end;" +
                        "Operation Main(); Procedure" +
                        "   Var F,X : Foo; Var I : Std_Integer_Fac :: Integer;" +
                        "   F.i:=32; X.i:=88; F.j:=4; X.j:=5;" +
                        "   Std_Integer_Fac :: Write_Line(F.i); " +
                        "   Std_Integer_Fac :: Write_Line(F.j); " +
                        "   Std_Integer_Fac :: Write_Line(X.i); " +
                        "   Std_Integer_Fac :: Write_Line(X.j); " +
                        "   F.i:=:X.i;" +
                        "   Std_Integer_Fac :: Write_Line(F.i); " +
                        "   Std_Integer_Fac :: Write_Line(F.j); " +
                        "   Std_Integer_Fac :: Write_Line(X.i); " +
                        "   Std_Integer_Fac :: Write_Line(X.j); " +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("32\n4\n88\n5\n88\n4\n32\n5\n", found);
    }*/
}
