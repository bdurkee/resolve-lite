package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.GenericSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;

import java.util.*;

public class ModuleParameterization {

    private final SymbolTable scopeRepo;
    private final String moduleID;

    private final List<ResolveParser.ModuleArgumentContext> arguments =
            new ArrayList<>();
    private final List<ProgTypeSymbol> actualGenerics = new ArrayList<>();
    private final FacilitySymbol instantiatingFacility;

    public ModuleParameterization(String moduleID,
            List<ProgTypeSymbol> actualGenerics,
            ResolveParser.ModuleArgumentListContext actualArgs,
            FacilitySymbol instantiatingFacility, SymbolTable scopeRepo) {
        this.instantiatingFacility = instantiatingFacility;
        this.scopeRepo = scopeRepo;
        this.actualGenerics.addAll(actualGenerics);

        if ( actualArgs != null ) {
            arguments.addAll(actualArgs.moduleArgument());
        }
        this.moduleID = moduleID;
    }

    public Scope getScope(boolean instantiated) {
        Scope result;
        try {
            ModuleScopeBuilder originalScope =
                    scopeRepo.getModuleScope(moduleID);
            result = originalScope;
            if ( instantiated ) {
                Map<String, PTType> genericInstantiations;

                genericInstantiations =
                        getGenericInstantiations(originalScope, arguments);
                result =
                        new InstantiatedScope(originalScope,
                                genericInstantiations, instantiatingFacility);
            }
        }
        catch (NoSuchSymbolException nsse) {
            //Shouldn't be possible--we'd have caught it by now
            throw new RuntimeException(nsse);
        }
        return result;
    }

    private Map<String, PTType> getGenericInstantiations(
            ModuleScopeBuilder moduleScope,
            List<ResolveParser.ModuleArgumentContext> actualArguments) {
        Map<String, PTType> result = new HashMap<>();

        List<GenericSymbol> formalGenerics =
                moduleScope.getSymbolsOfType(GenericSymbol.class);

        if ( formalGenerics.size() != actualGenerics.size() ) {
            throw new RuntimeException("generic list sizes do not match");
        }
        Iterator<ProgTypeSymbol> suppliedGenericIter =
                actualGenerics.iterator();
        Iterator<GenericSymbol> formalGenericIter = formalGenerics.iterator();
        while (formalGenericIter.hasNext()) {
            result.put(formalGenericIter.next().getName(), suppliedGenericIter
                    .next().getProgramType());
        }
        return result;
    }

    public String getName() {
        return moduleID;
    }

    public String getModuleID() {
        return moduleID;
    }

    public List<ResolveParser.ModuleArgumentContext> getArguments() {
        return arguments;
    }
}
