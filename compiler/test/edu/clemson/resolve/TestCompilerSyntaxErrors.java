package edu.clemson.resolve;

import edu.clemson.resolve.compiler.ErrorKind;
import org.junit.Test;

public class TestCompilerSyntaxErrors extends BaseTest {

    @Test public void testMissingModuleSemi() throws Exception {
        String[] pair = new String[] {
                "Precis T\n" +
                    "uses x,y,z;\n" +
                "end T;",
                "error(" + ErrorKind.SYNTAX_ERROR.code + "): T.resolve:2:0: syntax error: missing ';' at 'uses'",
        };
        super.testErrors(pair, "T");
    }

    @Test public void testMissingSemi2() throws Exception {
        String[] pair = new String[] {
                "Precis T\n" +
                        "uses x,y,z\n" +
                        "end T;",
                "error(" + ErrorKind.SYNTAX_ERROR.code + "): T.resolve:2:0: syntax error: missing ';' at 'uses'\n"+
                "error(" + ErrorKind.SYNTAX_ERROR.code + "): T.resolve:3:0: syntax error: extraneous input 'end' expecting {';', ','}",
        };
        super.testErrors(pair, "T");
    }
}
