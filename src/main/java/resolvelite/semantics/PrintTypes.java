package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

public class PrintTypes extends ResolveBaseListener {

    ParseTreeProperty<Type> types;

    public PrintTypes(ParseTreeProperty<Type> types) {
        this.types = types;
    }

    @Override
    public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        System.out.printf("%-17s", ctx.getText());
        System.out.printf(" type %-8s\n", types.get(ctx).toString()
                .toLowerCase());
    }
}
