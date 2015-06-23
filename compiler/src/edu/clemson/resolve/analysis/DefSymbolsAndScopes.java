package edu.clemson.resolve.analysis;

import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.misc.NotNull;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    @Override public void exitMathFunctionExp(
            @NotNull Resolve.MathFunctionExpContext ctx) {
    }
}
