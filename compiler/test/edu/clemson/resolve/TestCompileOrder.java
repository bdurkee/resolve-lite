package edu.clemson.resolve;

import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.junit.Assert;
import org.junit.Test;

public class TestCompileOrder extends BaseTest {

    @Test public void testSimpleOrderingNoErrors() throws Exception {
        String[] pair = new String[] {
                "Precis T; \n uses Boo; \n end T;",
                "populating: Bar\npopulating: Boo\npopulating: T"
        };
        super.testInfos(pair, "T", "-lib", "resolve/");
    }
}
