package edu.clemson.resolve.codegen;

import edu.clemson.resolve.BaseTest;
import org.junit.Assert;
import org.junit.Test;

public class TestMemberAccesses extends BaseTest {
    @Test public void testNestedMemberAccess() throws Exception {
        String[] modules = new String[]{
                "Facility T; uses Standard_Integers, Standard_Booleans;" +
                "Type Foo = Record x,y : Std_Integer_Fac :: Integer; end;" +
                "Type Goo = Record F : Foo; y : Std_Boolean_Fac :: Boolean; end;" +
                "Operation Main(); Procedure " +
                    "Var f : Foo; Var g : Goo; g.F.x:=3;" +
                    "g.F.y:=89; g.y:=Std_Boolean_Fac::True();" +
                    "Std_Integer_Fac :: Write_Line(g.F.x);" +
                    "Std_Integer_Fac :: Write_Line(g.F.y);" +
                    "Std_Boolean_Fac :: Write_Line(g.y);" +
                    "Std_Integer_Fac :: Write_Line(f.x);" +
                    "Std_Integer_Fac :: Write_Line(f.y);" +
                "end Main; end T;"
        };
        String facility = modules[0];
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("3\n89\ntrue\n0\n0\n", found);
    }

    @Test public void testConceptReprMemberAccess() throws Exception {
        String[] modules = new String[] {
                "Concept T; Operation Test(); Type Family Bar is modeled by B; exemplar b; end T;",
                "Implementation T_I for T; uses Standard_Integers; " +
                "Type Bar = Record x,y : Std_Integer_Fac :: Integer; end; " +
                "Procedure Test(); Var x : Bar; x.x:=3; x.y:=44; " +
                "Std_Integer_Fac :: Write(x.x); " +
                "Std_Integer_Fac :: Write(x.y); " +
                "end Test; end T_I;",
                "Facility U; uses Standard_Integers; Facility TF is T implemented by T_I; " +
                "Operation main(); Procedure TF :: Test(); end main; end U;"
        };
        writeModules(modules, "T", "T_I", "U");
        String found = execCode("U.resolve", modules[2], "U", false);
        Assert.assertEquals("344\n", found);
    }

    @Test public void testMemberAccessWithInitialization() throws Exception {
        String[] modules = new String[]{
                "Facility T; uses Standard_Integers, Standard_Booleans;" +
                        "Type Foo = Record x,y : Std_Integer_Fac :: Integer; end;" +
                        "initialization F.x:=33; F.y:=44; end;" +
                        "Operation Main(); Procedure Var F : Foo;" +
                        "Std_Integer_Fac :: Write_Line(F.x);" +
                        "Std_Integer_Fac :: Write_Line(F.y);" +
                        "end Main; end T;"
        };
        writeModules(modules, "T");
        String found = execCode("T.resolve", modules[0], "T", false);
        Assert.assertEquals("33\n44\n", found);
    }

    @Test public void testExcessivelyNestedMemberAccess() throws Exception {
        String[] modules = new String[]{
                "Facility T; uses Standard_Integers, Standard_Booleans;" +
                        "Type Moo = Record i,j : Std_Boolean_Fac :: Boolean; end;" +
                        "initialization M.i:= Std_Boolean_Fac :: True(); end;" +

                        "Type Doo = Record g : Moo; h : Std_Integer_Fac :: Integer; end;" +
                        "Type Goo = Record e : Doo; f : Std_Integer_Fac :: Integer; end;" +
                        "Type Boo = Record c : Goo; d : Std_Integer_Fac :: Integer; end;" +
                        "Type Foo = Record a : Boo; b : Std_Integer_Fac :: Integer; end;" +

                        "Operation Main(); Procedure Var F : Foo;" +
                        "Std_Boolean_Fac :: Write_Line(F.a.c.e.g.i);" +
                        "end Main; end T;"
        };
        writeModules(modules, "T");
        String found = execCode("T.resolve", modules[0], "T", false);
        Assert.assertEquals("true\n", found);
    }
}
