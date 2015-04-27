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

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        printProgTypesForExp(ctx);
    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        printProgTypesForExp(ctx);
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        printProgTypesForExp(ctx);
    }

    @Override public void exitMathInfixExp(
            @NotNull ResolveParser.MathInfixExpContext ctx) {
        printMathTypesForExp(ctx);
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        printMathTypesForExp(ctx);
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        printMathTypesForExp(ctx);
    }

    @Override public void exitMathTupleExp(
            @NotNull ResolveParser.MathTupleExpContext ctx) {
        printMathTypesForExp(ctx);
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        printMathTypesForExp(ctx);
    }

    private void printProgTypesForExp(ParserRuleContext ctx) {
        if ( tree.mathTypes.get(ctx) == null ) {
            throw new IllegalStateException("node: " + ctx.getText()
                    + " has a null math type");
        }
        if ( tree.progTypes.get(ctx) == null ) {
            throw new IllegalStateException("node: " + ctx.getText()
                    + " has a null prog type");
        }
        System.out.printf("%-17s", ctx.getText());
        System.out.printf(" progtype %-8s  mathtype %-8s\n",
                tree.progTypes.get(ctx), tree.mathTypes.get(ctx));
    }

    private void printMathTypesForExp(ParserRuleContext ctx) {
        if ( tree.mathTypes.get(ctx) == null ) {
            throw new IllegalStateException("node: " + ctx.getText()
                    + " has a null math type");
        }
        System.out.printf("%-17s", ctx.getText());
        System.out.printf(" mathtype %-8s  mathtype value %-8s\n",
                tree.mathTypes.get(ctx), tree.mathTypeValues.get(ctx));
    }

}
