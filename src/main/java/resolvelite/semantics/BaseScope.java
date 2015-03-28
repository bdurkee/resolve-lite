package resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.misc.Utils;
import resolvelite.semantics.symbol.FacilitySymbol;
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

    @Override public Set<String> getImports() {
        return Collections.emptySet();
    }

    @Override public Symbol getSymbol(String name) {
        return symbols.get(name);
    }

    @Override public void setEnclosingScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override public Symbol resolve(Token qualifier, Token name)
            throws NoSuchSymbolException {
        if (qualifier != null) {
            return qualifiedResolution(qualifier.getText(), name.getText());
        }
        else {
            return null;
            //return unqualifiedResolution(name);
        }
    }

    private Symbol qualifiedResolution(String qualifier, String name)
            throws NoSuchSymbolException {
        Symbol referencedFacility = null;
        try {
            //first look for a facility in the current modulescope with
            //name 'qualifier'
            Symbol f = this.resolve(qualifier);
            referencedFacility = (FacilitySymbol) f;
        }
        //maybe our facility is in one of our named imports
        catch (NoSuchSymbolException nsse) {
            for(String importedScope : this.getImports()) {
                try {
                    referencedFacility = scopeRepo.moduleScopes
                            .get(importedScope).resolve(qualifier);
                } catch (NoSuchSymbolException e) {
                    referencedFacility = null;
                }
            }
        }
        if (referencedFacility == null) {
            //ok maybe our qualifier is just referencing an imported module.
            String refModule = this.getImports().stream()
                    .filter(i -> i.equals(qualifier)).toString();
            scopeRepo.moduleScopes.get(refModule).resolve(name);
        }

        return null;
    }

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

}
