package edu.clemson.resolve.codegen;

import edu.clemson.resolve.BaseTest;
import org.junit.Test;
import org.stringtemplate.v4.ST;

public class TestCalls extends BaseTest {

    @Test public void testArglessOpCall() throws Exception {

        ST facilityST = new ST(
                "Facility T;" +
                    "Operation Foo(); Procedure \n end Foo;" +
                    "Operation Boo(); Procedure Foo(); end Boo;" +
                "end T;");
        String facility = facilityST.render();

        String input = "";
        String found = execCode("T.resolve", facility, "T", input, false);

    }
}
