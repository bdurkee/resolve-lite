package edu.clemson.resolve.codegen;

import edu.clemson.resolve.BaseTest;
import org.junit.Assert;
import org.junit.Test;

public class TestFacilityDecls extends BaseTest {

    @Test public void testUnenhancedFacilityDecl() throws Exception {
        String[] modules = new String[] {
                "Concept T;\n Operation Test(); end T;",
                "Implementation T_I for T; uses Standard_Char_Strings; Procedure Test(); " +
                        "  Std_Char_Str_Fac :: Write_Line(\"runningTest\");" +
                        "end Test; end T_I;",
                "Facility U; Facility TF is T implemented by T_I; " +
                        "Operation main(); Procedure TF :: Test();end main;end U;"
        };
        writeModules(modules, "T", "T_I", "U");
        String found = execCode("U.resolve", modules[2], "U", "", false);
        Assert.assertEquals("runningTest\n", found);
    }

    @Test public void testParameterizedUnenhancedFacilityDecl() throws Exception {
        String[] modules = new String[] {
                "Concept T<U>(evaluates i,j,k : Std_Integer_Fac :: Integer; " +
                    "Definition Is_LEQ(i,j : U) : B;);" +
                    "uses Standard_Booleans, Standard_Integers;" +
                    "Operation Op_Requiring_Predicate_P(); end T;",

                "Implementation T_I(Operation P(alters j,k : U ) : Std_Boolean_Fac :: Boolean;) for T; " +
                    "uses Standard_Booleans, Standard_Char_Strings; " +
                    "Procedure Op(evaluates i,j : U); " +
                    "If (P(i,j)) then Std_Char_Str_Fac :: Write_Line(\"P=true:\"); " +
                    "else Std_Char_Str_Fac :: Write_Line(\"P=false:\"); end\n" +
                    "end Op;end T_I;",

                "Facility U; uses Standard_Integers, Standard_Booleans, Basic_Integer_Theory;" +
                    "Definition Int_Leq(i, j : Z) : B;" +
                    "Operation My_Test(alters x,y : Std_Integer_Fac :: Integer) : Std_Boolean_Fac :: Boolean;" +
                    "Procedure My_Test:=x<=y;end My_Test;" +
                    "Facility TF is T<Std_Integer_Fac :: Integer>(0,1,2,Int_Leq) " +
                    "implemented by T_I(My_Test); " +
                    "Operation Main(); Procedure end Main; Var x, y : Std_Integer_Fac :: Integer;" +
                    "x:=4; y:=6; TF :: Op(x,y); end Main; end U;"
        };
        writeModules(modules, "T", "T_I", "U");
        String found = execCode("U.resolve", modules[2], "U", "", false);
        Assert.assertEquals("\n", found);
    }
}
