package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;

public class PrintTypes extends ResolveBaseListener {

    AnnotatedTree tree;

    public PrintTypes(AnnotatedTree t) {
        this.tree = t;
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        printTypes(ctx);
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        printTypes(ctx);
    }

    private void printTypes(ParserRuleContext ctx) {
        if ( tree.mathTypes.get(ctx) == null ) {
            throw new IllegalStateException("ctx: "
                    + ctx.getClass().getSimpleName() + " null");
        }
        System.out.printf("%-17s", ctx.getText());
        System.out.printf(" type %-8s  typevalue %-8s\n", getTypeStr(ctx),
                getTypeValueStr(ctx));
    }

    private String getTypeStr(ParseTree t) {
        return tree.mathTypes.get(t).toString();
    }

    private String getTypeValueStr(ParseTree t) {
        return tree.mathTypeValues.get(t) != null ? tree.mathTypeValues.get(t)
                .toString() : "null";
    }
}
