package org.resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.resolvelite.misc.Utils;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.ParameterSymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.semantics.symbol.TypedSymbol;

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

    @Override public Symbol resolve(Token qualifier, Token name,
            boolean searchImports) throws NoSuchSymbolException {
        return resolve(qualifier == null ? null : qualifier.getText(),
                name.getText(), searchImports);
    }

    @Nullable public Symbol resolve(String qualifier, String name,
            boolean searchImports) throws NoSuchSymbolException {
        if ( qualifier != null ) {
            return qualifiedSearch(qualifier, name, searchImports);
        }
        else {
            return unqualifiedSearch(name, searchImports);
        }
    }

    protected Symbol unqualifiedSearch(String name, boolean searchImports)
            throws NoSuchSymbolException {
        //first search locally...
        Symbol s = symbols.get(name);
        if ( s != null ) {
            //System.out.println("found "+name+" in "+this.asScopeStackString());
            return s;
        }
        Scope parent = getParentScope();
        if ( parent != null && !(parent instanceof PredefinedScope) ) {
            return parent.resolve(null, name, searchImports);
        }
        //if we get to here we were NOT able to find it locally...
        if ( !searchImports )
            throw new NoSuchSymbolException();
        else {
            ModuleScope m = (ModuleScope) this;
            //Todo: in the future we can collect a list and if it has more than we need we raise hell
            for (String importRef : m.getImports()) {
                try {
                    Symbol result = scopeRepo //
                            .getModuleScope(importRef) //
                            .resolve(null, name, false); //
                    if ( result != null ) return result; // found a match
                }
                catch (NoSuchSymbolException nsse) {
                    //no dice for the name in the imported module we've just
                    //searched? Ok then, lets check for it in any facilityVars.
                    //that might be available.
                    //no problem, just keep searching until we find a match.
                }
            }
        }
        //Two options here.
        // 1. Make this version work, where I can find facilityVars,
        //search them in diff modules etc.
        // 2. Enforce qualification and make it so that you need to
        throw new NoSuchSymbolException();
    }

    /*@Override public Symbol resolve(String name) throws NoSuchSymbolException {
        Symbol s = symbols.get(name);
        if ( s != null ) {
            //System.out.println("found "+name+" in "+this.asScopeStackString());
            return s;
        }
        Scope parent = getParentScope();
        if ( parent != null ) return parent.resolve(name);
        throw new NoSuchSymbolException(name);
    }*/

    protected Symbol qualifiedSearch(String qualifier, String name,
            boolean searchImports) throws NoSuchSymbolException {
        FacilitySymbol referencedFacility = null;
        try {
            //first look for a facility in the current modulescope with
            //name 'qualifier'
            Symbol f = this.resolve(null, qualifier, false);
            referencedFacility = (FacilitySymbol) f;
        }
        //maybe the fac referenced by qualifier is in one of our named imports?
        catch (NoSuchSymbolException nsse) {
            if ( !searchImports ) throw new NoSuchSymbolException();
            ModuleScope thisModule =
                    scopeRepo.getModuleScope(this.getRootModuleID());
            for (String referencedModule : thisModule.getImports()) {
                try {
                    /// scopeRepo.getModuleScope(this.g);
                    referencedFacility = //
                            (FacilitySymbol) scopeRepo //
                                    .getModuleScope(referencedModule) //
                                    .resolve(null, qualifier, false); //
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
            return scopeRepo.getModuleScope(qualifier).resolve(null, name,
                    false);
            //  }
            //  else {
            //      throw new NoSuchSymbolException();
            //  }
        }
        else {
            //we've found the referenced facility, let's search it to see if we
            //can find the requested symbol, 'name'.
            return scopeRepo.getModuleScope(referencedFacility.getSpecName())
                    .resolve(null, name, false);
        }
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

    @Override public List<Scope> getNestedScopes() {
        return getSymbols()
                .stream()
                .filter(s -> s instanceof Scope)
                .map(s -> (Scope)s).collect(Collectors.toList());
    }

    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return symbols.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
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
