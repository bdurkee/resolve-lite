package resolvelite.typeandpopulate;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.compiler.ErrorManager;
import resolvelite.parsing.ResolveParser;

public class ModuleIdentifier implements Comparable<ModuleIdentifier> {
    public static final ModuleIdentifier GLOBAL = new ModuleIdentifier();

    @NotNull
    private final String name;
    private final boolean globalFlag;

    private ModuleIdentifier() {
        this.name = "GLOBAL";
        this.globalFlag = true;
    }

    public ModuleIdentifier(String s) {
        this.name = s;
        this.globalFlag = false;
    }

    public ModuleIdentifier(@NotNull ParserRuleContext ctx) {
        this(getModuleNameRuleCtx(ctx));
    }

    public ModuleIdentifier(@NotNull Token t) {
        this(t.getText());
    }

    private static Token getModuleNameRuleCtx(@NotNull ParserRuleContext ctx) {
        Token result = null;
        if (ctx instanceof ResolveParser.PrecisModuleContext) {
            ResolveParser.PrecisModuleContext ctxAsPrecis =
                    (ResolveParser.PrecisModuleContext) ctx;
            result = ctxAsPrecis.name;
        }
        else {
            ErrorManager.fatalInternalError("unrecognized module: "
                    + ctx.getText(), new IllegalArgumentException());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof ModuleIdentifier);

        if (result) {
            result = ((ModuleIdentifier) o).name.equals(this.name);
        }
        return result;
    }

    @Override
    public int compareTo(ModuleIdentifier o) {
        return name.compareTo(o.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return name;
    }

    public String fullyQualifiedRepresentation(String symbol) {
        return name + "." + symbol;
    }
}
