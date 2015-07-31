package edu.clemson.resolve.codegen;

import edu.clemson.resolve.BaseTest;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestCalls extends BaseTest {

    @Test public void testArglessOpCall() throws Exception {
        mkdir(tmpdir);

        ST facilityST = new ST(
                "Facility T; uses Standard_Types, Standard_IO;" +
                "Operation Foo(); Procedure \n end Foo;" +
                "Operation Main(); Procedure Foo(); end Main;" +
                "end T;");
        String facility = facilityST.render();

        String input = "";
        String found = execCode("T.resolve", facility, "T", input, false);

    }


}
