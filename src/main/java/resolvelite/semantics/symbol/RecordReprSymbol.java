package resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

public class RecordReprSymbol extends AbstractReprSymbol {

    public RecordReprSymbol(String name, ParserRuleContext tree) {
        super(name, tree);
    }

    public RecordReprSymbol(String name) {
        this(name, null);
    }

    @Override
    public void define(Symbol sym) throws IllegalArgumentException {
        if ( !(sym instanceof VariableSymbol) ) {
            throw new IllegalArgumentException("sym is "
                    + sym.getClass().getSimpleName() + ", not VariableSymbol");
        }
        super.define(sym);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VariableSymbol> getSymbols() {
        return (List<VariableSymbol>) super.getSymbols();
    }
}
