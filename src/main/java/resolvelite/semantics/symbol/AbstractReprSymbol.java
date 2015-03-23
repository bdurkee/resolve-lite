package resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import resolvelite.semantics.Scope;
import resolvelite.semantics.Type;

public abstract class AbstractReprSymbol extends SymbolWithScope
        implements
            Type {

    protected ParserRuleContext tree;

    public AbstractReprSymbol(String name, ParserRuleContext tree) {
        super(name);
        this.tree = tree;
    }

    public AbstractReprSymbol(String name) {
        this(name, null);
    }
}
