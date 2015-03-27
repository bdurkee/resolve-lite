package resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.misc.Utils;
import resolvelite.semantics.symbol.ParameterSymbol;
import resolvelite.semantics.symbol.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseScope implements Scope {

    protected Scope enclosingScope; // null if predefined (outermost) scope

    protected Map<String, Symbol> symbols = new LinkedHashMap<>();
    protected final SymbolTable scopeRepo;

    public BaseScope(SymbolTable scopeRepo) {
        this(null, scopeRepo);
    }

    public BaseScope(Scope enclosingScope, SymbolTable scopeRepo) {
        setEnclosingScope(enclosingScope);
        this.scopeRepo = scopeRepo;
    }

    @Override public Symbol getSymbol(String name) {
        return symbols.get(name);
    }

    @Override public void setEnclosingScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override public Symbol resolve(Token qualifier, Token name)
            throws NoSuchSymbolException {
        Symbol result = null;
        if (qualifier != null) {
            ModuleScope referencedModule =
                    scopeRepo.getModuleScope(qualifier.getText());
            result = referencedModule.resolve(name.getText());
        }
        else {
            result = resolve(name.getText());
        }
        return result;
    }

    /*@Override public Symbol resolve(String name, boolean searchImports)
            throws NoSuchSymbolException {
        Symbol s = symbols.get(name);
        if ( s != null ) {
            //System.out.println("found "+name+" in "+this.asScopeStackString());
            return s;
        }
        // if not here, check any enclosing scope
        if (this instanceof ModuleScope && searchImports) {
            ModuleScope module = (ModuleScope) this;
            for (String referencedImport : module.getImportedModules()) {
                ModuleScope ref = scopeRepo.moduleScopes.get(referencedImport);
                return ref.resolve(name, false); // only search one level for now.
            }
        }
        Scope parent = getParentScope();
        if ( parent != null ) {
            return parent.resolve(name);
        }
        throw new NoSuchSymbolException(name);
    }*/

    @Override public Symbol resolve(String name) throws NoSuchSymbolException {
        Symbol s = symbols.get(name);
        if ( s != null ) {
            //System.out.println("found "+name+" in "+this.asScopeStackString());
            return s;
        }
        Scope parent = getParentScope();
        if ( parent != null ) return parent.resolve(name);
        throw new NoSuchSymbolException(name);
    }

    @Override public void define(@NotNull Symbol sym)
            throws DuplicateSymbolException {
        if ( symbols.containsKey(sym.getName()) ) {
            throw new DuplicateSymbolException();
        }
        //Note that we set the enclosing scopes here
        sym.setScope(this);
        symbols.put(sym.getName(), sym);
    }

    @Override public Scope getParentScope() {
        return getEnclosingScope();
    }

    @Override public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override public List<? extends Symbol> getSymbols() {
        return new ArrayList<>(symbols.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Scope> getNestedScopes() {
        List<? extends Symbol> scopes =
                Utils.filter(getSymbols(), s -> s instanceof Scope);
        return (List)scopes; // force it to cast
    }

    @Override public int getNumberOfSymbols() {
        return symbols.size();
    }

    @Override public Set<String> getSymbolNames() {
        return symbols.keySet();
    }

    @Override public String toString() {
        return getScopeDescription() + ":" + symbols.keySet().toString();
    }

    /*public String toTestString() {
        return toTestString(", ", ".");
    }

    public String toTestString(String separator, String scopePathSeparator) {
        List<? extends Symbol> allSymbols = this.getAllSymbols();
        List<String> syms = Utils.map(allSymbols, s ->
                s.getScope().getScopeDescription()
                        + scopePathSeparator + s.getName());
        return Utils.join(syms, separator);
    }*/
}
