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
    protected final String rootModuleID;

    public BaseScope(SymbolTable scopeRepo, String rootModuleID) {
        this(null, scopeRepo, rootModuleID);
    }

    public BaseScope(Scope enclosingScope, SymbolTable scopeRepo,
            String rootModuleID) {
        setEnclosingScope(enclosingScope);
        this.scopeRepo = scopeRepo;
        this.rootModuleID = rootModuleID;
    }

    @Override public Symbol getSymbol(String name) {
        return symbols.get(name);
    }

    @Override public void setEnclosingScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override public Symbol resolve(Token qualifier, Token name)
            throws NoSuchSymbolException {
        if ( qualifier != null ) {
            return qualifiedResolution(qualifier.getText(), name.getText());
        }
        else {
            //for now just a local modulescope resolve.
            return resolve(name.getText());
        }
    }

    private Symbol qualifiedResolution(String qualifier, String name)
            throws NoSuchSymbolException {
        FacilitySymbol referencedFacility = null;
        try {
            //first look for a facility in the current modulescope with
            //name 'qualifier'
            Symbol f = this.resolve(qualifier);
            referencedFacility = (FacilitySymbol) f;
        }
        //maybe the fac referenced by qualifier is in one of our named imports?
        catch (NoSuchSymbolException nsse) {
            ModuleScope thisModule =
                    scopeRepo.getModuleScope(this.getRootModuleID());
            for (String referencedModule : thisModule.getImports()) {
                try {
                    /// scopeRepo.getModuleScope(this.g);
                    referencedFacility = //
                            (FacilitySymbol) scopeRepo //
                                    .getModuleScope(referencedModule) //
                                    .resolve(qualifier); //
                    if ( referencedFacility != null ) break;
                }
                catch (ClassCastException | NoSuchSymbolException cce) {
                    referencedFacility = null;
                }
            }
        }
        if ( referencedFacility == null ) {
            //ok maybe our qualifier is just referencing a named
            //(imported) module.
            //  if (scopeRepo.getModuleScope(this.getRootModuleID())
            //         .getImports().contains(qualifier)) {

            //Todo: if the qualifier isn't in the list of module imports,
            //then we technically shouldn't grab the modulescope for it.
            return scopeRepo.getModuleScope(qualifier).resolve(name);
            //  }
            //  else {
            //      throw new NoSuchSymbolException();
            //  }
        }
        else {
            //we've found the referenced facility, let's search it to see if we
            //can find the requested symbol, 'name'.
            return scopeRepo.getModuleScope(referencedFacility.getSpecName())
                    .resolve(name);
        }
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

    @Override public String getRootModuleID() {
        return rootModuleID;
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
