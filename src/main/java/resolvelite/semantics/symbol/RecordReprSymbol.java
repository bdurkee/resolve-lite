package resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import resolvelite.semantics.DuplicateSymbolException;
import resolvelite.semantics.NoSuchSymbolException;
import resolvelite.semantics.SymbolTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordReprSymbol extends AbstractReprSymbol {

    private final Map<String, VariableSymbol> fields = new HashMap<>();

    public RecordReprSymbol(String name, ParserRuleContext tree,
            SymbolTable scopeRepo, String rootModuleID) {
        super(name, tree, scopeRepo, rootModuleID);
    }

    public RecordReprSymbol(String name, SymbolTable scopeRepo,
            String rootModuleID) {
        this(name, null, scopeRepo, rootModuleID);
    }

    @Override public void define(Symbol sym) throws DuplicateSymbolException {
        if ( !(sym instanceof VariableSymbol) ) {
            throw new IllegalArgumentException("sym is "
                    + sym.getClass().getSimpleName() + ", not VariableSymbol");
        }
        fields.put(sym.getName(), (VariableSymbol) sym);
        super.define(sym);
    }

    /**
     * Resolves a reference to an {@link VariableSymbol} living within this
     * record. We don't use {@link #resolve(String)} since we don't want to
     * search for member references up the scope hierarchy---only here.
     * 
     * @param name The member referenced
     * @return The {@link VariableSymbol} referenced by <code>name</code>.
     * 
     * @throws NoSuchSymbolException If the reference doesn't exist or couldn't
     *         be resolved.
     */
    public VariableSymbol resolveMember(String name)
            throws NoSuchSymbolException {
        VariableSymbol result = fields.get(name);
        if ( result == null ) throw new NoSuchSymbolException();
        return result;
    }

    @Override @SuppressWarnings("unchecked") public List<VariableSymbol>
            getSymbols() {
        return (List<VariableSymbol>) super.getSymbols();
    }

}
