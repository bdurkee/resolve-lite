package edu.clemson.resolve.analysis;

import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MathClassification;

public class MathClassificationPrintingListener extends ResolveBaseListener {

    @NotNull private final ParseTreeProperty<MathClassification> types;

    public MathClassificationPrintingListener(
            @NotNull ParseTreeProperty<MathClassification> types) {
        this.types = types;
    }

    @Override public void enterMathTypeExp(
            ResolveParser.MathTypeExpContext ctx) {
        System.out.println("----------[enterMathTypeExp]");
    }

    @Override public void exitMathTypeExp(
            ResolveParser.MathTypeExpContext ctx) {
        System.out.println("----------[exitMathTypeExp]");
    }

    @Override public void exitMathPrefixAppExp(
            ResolveParser.MathPrefixAppExpContext ctx) {
        printType(ctx);
    }

    @Override public void exitMathInfixAppExp(
            ResolveParser.MathInfixAppExpContext ctx) {
        printType(ctx);
    }

    @Override public void exitMathSymbolExp(
            ResolveParser.MathSymbolExpContext ctx) {
        printType(ctx);
    }

    @Override public void exitMathBooleanLiteralExp(
            ResolveParser.MathBooleanLiteralExpContext ctx) {
        printType(ctx);
    }

    @Override public void exitMathIntegerLiteralExp(
            ResolveParser.MathIntegerLiteralExpContext ctx) {
        printType(ctx);
    }

    @Override public void exitMathQuantifiedExp(
            ResolveParser.MathQuantifiedExpContext ctx) {
        printType(ctx);
    }

    @Override public void exitMathSetRestrictionExp(
            ResolveParser.MathSetRestrictionExpContext ctx) {
        printType(ctx);
    }

    @Override public void exitMathCartProdExp(
            ResolveParser.MathCartProdExpContext ctx) {
        printType(ctx);
    }

    private void printType(@NotNull ParserRuleContext ctx) {
        MathClassification t = types.get(ctx);
        if ( t == null ) {
            System.out.println("["+ctx.getClass().getSimpleName()+"]"+ctx.getText() + " : null");
            return;
        }
        String colonOp = " : ";
        if (t == t.getTypeGraph().CLS) {
            colonOp = " ː ";
        }
        System.out.println("["+ctx.getClass().getSimpleName()+"]"+
                ctx.getText()+colonOp+t+"  <"+t.getClass().getSimpleName()+">");
    }
}
