package edu.clemson.resolve;

import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCompileOrder extends BaseTest {

    @Test public void testBasicLinearOrdering() throws Exception {
        String[] modules = new String[] {
                "Precis T;\n uses U;\n end T;",
                "Precis U;\n uses V;\n end U;",
                "Precis V;\n end V;"
        };
        String expected = "populating: V\npopulating: U\npopulating: T";
        writeModules(modules, "T", "U", "V");
        testOrdering(expected, "T");
    }

    @Test public void testTrivialOrdering() throws Exception {
        String[] modules = new String[] {
                "Precis T;\n end T;",
        };
        String expected = "populating: T";
        writeModules(modules, "T");
        testOrdering(expected, "T");
    }

    @Test public void testTrivialOrdering2() throws Exception {
        String[] modules = new String[] {
                "Precis T;\n uses U;\n end T;",
                "Precis U;\n end U;",
        };
        String expected = "populating: U\npopulating: T";
        writeModules(modules, "T", "U");
        testOrdering(expected, "T");
    }

    @Test public void testFlawedOrdering() throws Exception {
        String[] modules = new String[] {
                "Precis U;\n uses X;\n end U;",
                "Precis X;\n uses Y, V;\n end U;",
                "Precis V;\n uses U;\n end V;"
        };
        String[] pairs = new String[] {
                "Precis Flawed;\n uses U;\n end Flawed;",
                "error(" + ErrorKind.MISSING_IMPORT_FILE.code + "): X.resolve:2:6: module X was unable to find the file corresponding to uses reference 'Y'" + "\n"+
                "error(" + ErrorKind.CIRCULAR_DEPENDENCY.code + "): V.resolve:2:6: circular dependency: U depends on V, but V also depends on U"
        };
        writeModules(modules, "U", "X", "V");
        super.testErrors(pairs, "Flawed");
    }

    //Todo: When facilities, enhancements, and other constructs are
    //are eventually added, we're going to want to test compilation ordering
    //on the things they implicitly import.

    private void testOrdering(String expected, String root) {
        ErrorCollector e = resolve(root+RESOLVECompiler.FILE_EXTENSION, false);
        assertEquals(expected, e.toInfoString());
    }
}
