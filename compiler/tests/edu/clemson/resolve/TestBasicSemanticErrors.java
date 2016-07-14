package edu.clemson.resolve;

import edu.clemson.resolve.compiler.ErrorKind;
import org.antlr.v4.tool.ErrorType;
import org.junit.Test;

public class TestBasicSemanticErrors extends BaseTest {

    @Test
    public void testCiruclarDependency1() throws Exception {
        String module =
                "Facility T;\n" +
                    "uses T;\n" +
                "end T;";
        String expected =
                "error(" + ErrorKind.CIRCULAR_DEPENDENCY.code + "): T.resolve:2:5: circular dependency: " +
                        "T depends on T, but T also depends on T";
        testErrors(new String[] {module, expected}, "T");
    }

    @Test
    public void testCiruclarDependency2() throws Exception {
        String[] modules = new String[]{
                "Precis T;\nuses U;\n end T;",
                "Precis U;\nuses X, Y; end U;",
                "Precis Y;\nend Y;",
                "Precis X;\nuses T; end X;",
        };
        writeModules(modules, "T", "U", "Y", "X");
        String expected =
                "error(" + ErrorKind.CIRCULAR_DEPENDENCY.code + "): X.resolve:2:5: circular dependency: " +
                        "X depends on T, but T also depends on X";
        testErrors(new String[] {modules[0], expected}, "T");
    }
}
