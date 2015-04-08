package org.resolvelite.semantics.symbol;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FacilitySymbol extends BaseSymbol {

    private final Map<GenericSymbol, Type> genericSubstitutes = new HashMap<>();
    private final String specName, implName;
    private final SymbolTable symtab;
    private final List<ResolveParser.TypeContext> actualGenerics =
            new ArrayList<>();

    public FacilitySymbol(String name, String specName, String implName,
            String rootModuleID, List<ResolveParser.TypeContext> actualGenerics,
            SymbolTable symtab) {
        super(name, rootModuleID);
        this.specName = specName;
        this.implName = implName;
        this.symtab = symtab;
        this.actualGenerics.addAll(actualGenerics);
    }

    public ModuleScope getModuleScopeWithGenericsSubstituted() {
        try {
            ModuleScope specScope = symtab.getModuleScope(specName);
            List<GenericSymbol> formalGenerics =
                    specScope.getSymbolsOfType(GenericSymbol.class);

            ModuleScope specScopeWithGenericsSubstituted =
                    new ModuleScope(symtab.getGlobalScope(), symtab,
                            specScope.getWrappedModuleTree());
            for (ParameterSymbol p :
                    specScope.getSymbolsOfType(ParameterSymbol.class)) {
                try {
                    specScope.define()
                } catch (DuplicateSymbolException dse) {
                    //shouldn't happen, working with a fresh scope.
                }
            }
        }
        catch (NoSuchSymbolException nsse) {

        }
    }

    public Map<GenericSymbol, Type> getGenericSubstitutions() {
        //first, get the scope of the specification
        try {
            ModuleScope specScope = symtab.getModuleScope(specName);
            List<GenericSymbol> formalGenerics =
                    specScope.getSymbolsOfType(GenericSymbol.class);

            List<ResolveParser.TypeContext>
        }
        catch (NoSuchSymbolException nsse) {

        }
        return genericSubstitutes;
    }

    public String getSpecName() {
        return specName;
    }

    public String getImplName() {
        return implName;
    }

    //Todo: Implement substituteGenerics on a facility
}
