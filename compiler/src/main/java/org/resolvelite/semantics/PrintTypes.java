package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;

public class PrintTypes extends ResolveBaseListener {

    ParseTreeProperty<MTType> types, typeValues;

    public PrintTypes(ParseTreeProperty<MTType> types,
            ParseTreeProperty<MTType> typeValues) {
        this.types = types;
        this.typeValues = typeValues;
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        printMathTypeStuff(ctx);
    }

    private void printMathTypeStuff(ParserRuleContext ctx) {
        if ( types.get(ctx) == null ) {
            throw new IllegalStateException("ctx: "
                    + ctx.getClass().getSimpleName() + " null");
        }
        System.out.printf("%-17s", ctx.getText());
        System.out.printf(" type %-8s  typevalue %-8s\n", getTypeStr(ctx),
                getTypeValueStr(ctx));
    }

    private String getTypeStr(ParseTree t) {
        return types.get(t).toString();
    }

    private String getTypeValueStr(ParseTree t) {
        return typeValues.get(t) != null ? typeValues.get(t).toString()
                : "null";
    }
}
