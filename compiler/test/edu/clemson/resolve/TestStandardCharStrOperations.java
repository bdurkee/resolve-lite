package edu.clemson.resolve;

import org.junit.Assert;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestStandardCharStrOperations extends BaseTest {

    @Test public void testStringMerger() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Operation Main(); Procedure" +
                        "   Std_Char_Str_Fac :: Write_Line(\"cat\"++\"Dog\"); " +
                        "   Std_Char_Str_Fac :: Write_Line(\"cat\"++\"\"); " +
                        "   Std_Char_Str_Fac :: Write_Line(\"\"++\"\"); " +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("catDog\ncat\n\n", found);
    }

    @Test public void testLength() throws Exception {
        ST facilityST = new ST(
                "Facility T; uses Standard_Integers, Standard_Char_Strings;" +
                        "Operation Main(); Procedure" +
                        "   Var x : Std_Char_Str_Fac :: Char_Str;"+
                        "   Std_Integer_Fac :: Write_Line(Std_Char_Str_Fac::Length(x)); " +
                        "   x++\"\";"+
                        "   Std_Integer_Fac :: Write_Line(Std_Char_Str_Fac::Length(x)); " +
                        "   x:=x++\"\";"+
                        "   Std_Integer_Fac :: Write_Line(Std_Char_Str_Fac::Length(x)); " +
                        "   x:=x++\"cat!fish\";"+
                        "   Std_Integer_Fac :: Write_Line(Std_Char_Str_Fac::Length(x)); " +
                        "end Main; end T;");
        String facility = facilityST.render();
        String found = execCode("T.resolve", facility, "T", false);
        Assert.assertEquals("0\n0\n0\n8\n", found);
    }
}
