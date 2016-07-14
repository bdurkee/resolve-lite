package edu.clemson.resolve.analysis;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathClssftn;

class MathClssftnLogger extends ResolveBaseListener {

    private final ParseTreeProperty<MathClssftn> types;
    private final RESOLVECompiler compiler;

    MathClssftnLogger(@NotNull RESOLVECompiler rc, @NotNull ParseTreeProperty<MathClssftn> types) {
        this.types = types;
        this.compiler = rc;
    }

    @Override
    public void enterMathClssftnExp(ResolveParser.MathClssftnExpContext ctx) {
        compiler.log("----------[exitMathTypeExp]");
    }

    @Override
    public void exitMathClssftnExp(ResolveParser.MathClssftnExpContext ctx) {
        compiler.log("----------[exitMathTypeExp]");
    }

    @Override
    public void exitMathPrefixAppExp(ResolveParser.MathPrefixAppExpContext ctx) {
        logClssftn(ctx);
    }

    @Override
    public void exitMathInfixAppExp(ResolveParser.MathInfixAppExpContext ctx) {
        logClssftn(ctx);
    }

    @Override
    public void exitMathSymbolExp(ResolveParser.MathSymbolExpContext ctx) {
        logClssftn(ctx);
    }

    @Override
    public void exitMathQuantifiedExp(ResolveParser.MathQuantifiedExpContext ctx) {
        logClssftn(ctx);
    }

    @Override
    public void exitMathSetRestrictionExp(ResolveParser.MathSetRestrictionExpContext ctx) {
        logClssftn(ctx);
    }

    @Override
    public void exitMathSetExp(ResolveParser.MathSetExpContext ctx) {
        logClssftn(ctx);
    }

    @Override
    public void exitMathCartProdExp(ResolveParser.MathCartProdExpContext ctx) {
        logClssftn(ctx);
    }

    @Override
    public void exitMathSelectorExp(ResolveParser.MathSelectorExpContext ctx) {
        logClssftn(ctx);
    }

    private void logClssftn(@NotNull ParserRuleContext ctx) {
        MathClssftn t = types.get(ctx);
        if (t == null) {
            compiler.log("[" + ctx.getClass().getSimpleName() + "]" + ctx.getText() + " : null");
            return;
        }
        String colonOp = " : ";
        if (t == t.getTypeGraph().CLS) {
            colonOp = " ː ";
        }
        compiler.log("[" + ctx.getClass().getSimpleName() + "]" +
                ctx.getText() + colonOp + t + "  <" + t.getClass().getSimpleName() + ">");
    }
}
