package edu.clemson.resolve;

import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCompileOrder extends BaseTest {

    @Test public void testSimpleOrderingNoErrors() throws Exception {
        String[] modules = new String[] {
                "Precis T; \n uses U; \n end T;",
                "Precis U; \n uses V; \n end U;",
                "Precis V; \n end V;"
        };
        String expected = "populating: V\npopulating: U\npopulating: T";
        String rootFileName = writeModules(modules, "T", "U", "V");
        testOrdering(expected, rootFileName);
    }

    private void testOrdering(String expected, String fileName) {
        ErrorCollector e = resolve(fileName, false);
        assertEquals(expected, e.toInfoString());
    }
}
