package edu.clemson.resolve;

import edu.clemson.resolve.misc.LogManager;
import edu.clemson.resolve.misc.Utils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestCompileOrder extends BaseTest {

    @Test
    public void testBasicLinearOrdering() throws Exception {
        String[] modules = new String[]{
                "Precis T;\n uses U;\n end T;",
                "Precis U;\n uses V;\n end U;",
                "Precis V;\n end V;"
        };
        String expected = "populating: V\npopulating: U\npopulating: T";
        writeModules(modules, "T", "U", "V");
        testOrdering(expected, "T");
    }

    /*    @Test
        public void testTrivialOrdering() throws Exception {
            String[] modules = new String[]{
                    "Precis T;\n end T;",
            };
            String expected = "populating: T";
            writeModules(modules, "T");
            testOrdering(expected, "T");
        }

        @Test
        public void testTrivialOrdering2() throws Exception {
            String[] modules = new String[]{
                    "Precis T;\n uses U;\n end T;",
                    "Precis U;\n end U;",
            };
            String expected = "populating: U\npopulating: T";
            writeModules(modules, "T", "U");
            testOrdering(expected, "T");
        }
    */
    //Todo: When facilities, enhancements, and other constructs are
    //are eventually added, we're going to want to returnEnsuresArgSubstitutions compilation ordering
    //on the things they implicitly import.
    public void testOrdering(String expected, String root) {
        ErrorQueue e = resolve(root + RESOLVECompiler.FILE_EXTENSION, false);
        LogManager l = e.compiler.logMgr;
        List<String> msgs = Utils.apply(l.getRecords(), LogManager.Record::getMsg);
        String actual = Utils.join(msgs, "\n");
        assertEquals(expected, actual);
    }
}
