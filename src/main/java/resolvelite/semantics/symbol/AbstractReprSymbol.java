package resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import resolvelite.semantics.Scope;
import resolvelite.semantics.SymbolTable;
import resolvelite.semantics.Type;

public abstract class AbstractReprSymbol extends SymbolWithScope
        implements
            Type {

    protected ParserRuleContext tree;

    public AbstractReprSymbol(String name, ParserRuleContext tree,
            SymbolTable scopeRepo, String rootModuleID) {
        super(name, scopeRepo, rootModuleID);
        this.tree = tree;
    }

    public AbstractReprSymbol(String name, SymbolTable scopeRepo,
            String rootModuleID) {
        this(name, null, scopeRepo, rootModuleID);
    }
}
