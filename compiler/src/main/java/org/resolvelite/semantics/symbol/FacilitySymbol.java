package org.resolvelite.semantics.symbol;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.*;

import java.util.*;
import java.util.stream.Collectors;

public class FacilitySymbol extends BaseSymbol {
    private final String specName, implName;
    private final SymbolTable symtab;
    private final List<ResolveParser.TypeContext> actualGenerics =
            new ArrayList<>();

    public FacilitySymbol(String name, String specName, String implName,
            String rootModuleID,
            List<ResolveParser.TypeContext> actualGenerics, SymbolTable symtab) {
        super(name, rootModuleID);
        this.specName = specName;
        this.implName = implName;
        this.symtab = symtab;
        this.actualGenerics.addAll(actualGenerics);
    }

    public ModuleScope getModuleScopeWithGenericsSubstituted()
            throws IllegalStateException {
        ModuleScope specScopeWithGenericsSubstituted = null;
       /* if (!symtab.definitionPhaseComplete) {
            throw new IllegalStateException("Can't instantiate generics before "
                    + "the symbol definition phase is complete");
        }*/
        try {
            ModuleScope specScope = symtab.getModuleScope(specName);
            Iterator<GenericSymbol> formalGenerics =
                    specScope.getSymbolsOfType(GenericSymbol.class).iterator();
            Iterator<Type> actualTypeIter = actualGenerics.stream()
                    .map(symtab.types::get).collect(Collectors.toList()).iterator();
            Map<GenericSymbol, Type> genericSubstitutions = new HashMap<>();
            while (actualTypeIter.hasNext()) {
                genericSubstitutions.put(formalGenerics.next(),
                        actualTypeIter.next());
            }
            specScopeWithGenericsSubstituted =
                    new ModuleScope(symtab.getGlobalScope(), symtab,
                            specScope.getWrappedModuleTree());

            for (Symbol s : specScope.getSymbols()) {
                try {
                    specScopeWithGenericsSubstituted.define(
                            s.substituteGenerics(genericSubstitutions,
                                    specScopeWithGenericsSubstituted));
                } catch (DuplicateSymbolException dse) {
                    //shouldn't happen, working with a fresh scope.
                }
            }
        }
        catch (NoSuchSymbolException nsse) {
        }
        return specScopeWithGenericsSubstituted;
    }

    public String getSpecName() {
        return specName;
    }

    public String getImplName() {
        return implName;
    }

    //Todo: Implement substituteGenerics on a facility
}
