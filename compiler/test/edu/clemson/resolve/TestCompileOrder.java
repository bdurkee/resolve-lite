package edu.clemson.resolve;

import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCompileOrder extends BaseTest {

    @Test public void testSimpleChainOrdering() throws Exception {
        String[] modules = new String[] {
                "Precis T; \n uses U; \n end T;",
                "Precis U; \n uses V; \n end U;",
                "Precis V; \n end V;"
        };
        String expected = "populating: V\npopulating: U\npopulating: T";
        writeModules(modules, "T", "U", "V");
        testOrdering(expected, "T");
    }

    @Test public void testTrivialModuleOrdering() throws Exception {
        String[] modules = new String[] {
                "Precis T; \n end T;",
        };
        String expected = "populating: T";
        writeModules(modules, "T");
        testOrdering(expected, "T");
    }

    private void testOrdering(String expected, String root) {
        ErrorCollector e = resolve(root+RESOLVECompiler.FILE_EXTENSION, false);
        assertEquals(expected, e.toInfoString());
    }
}
