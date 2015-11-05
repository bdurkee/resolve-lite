package edu.clemson.resolve;

import edu.clemson.resolve.compiler.ErrorKind;
import org.junit.Test;

//NOTE: Don't go writing too many of these until the grammar is
//relatively stable and complete. The reason being is that items within the
//{ ... } might change order given some change which will throw off
//some/most test cases. This is only because the lexer has undergone
//major revisions. But it's good to know anyways.
public class TestCompilerSyntaxErrors extends BaseTest {

    @Test public void testMissingSemi() throws Exception {
        String[] pair = new String[] {
                "Precis T\n" +
                    "uses x,y,z;\n" +
                "end T;",
                "error(" + ErrorKind.SYNTAX_ERROR.code + "): T.resolve:2:0: syntax error: missing ';' at 'uses'",
        };
        super.testErrors(pair, "T");
    }

    /*@Test public void testMissingSemi2() throws Exception {
        String[] pair = new String[] {
                "Precis T\n" +
                        "uses x,y,z\n" +
                        "end T;",
                "error(" + ErrorKind.SYNTAX_ERROR.code + "): T.resolve:2:0: syntax error: missing ';' at 'uses'\n"+
                "error(" + ErrorKind.SYNTAX_ERROR.code + "): T.resolve:3:0: syntax error: extraneous input 'end' expecting {',', ';'}",
        };
        super.testErrors(pair, "T");
    }*/
}
