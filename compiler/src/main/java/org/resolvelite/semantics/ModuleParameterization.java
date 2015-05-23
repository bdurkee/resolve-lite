package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.GenericSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;

import java.util.*;

public class ModuleParameterization {

    private final SymbolTable scopeRepo;
    private final String moduleID;

    private final List<ResolveParser.ModuleArgumentContext> arguments =
            new ArrayList<>();
    private final List<ResolveParser.TypeContext> suppliedGenerics =
            new ArrayList<>();
    private final FacilitySymbol instantiatingFacility;
    private final AnnotatedTree annotations;

    public ModuleParameterization(String moduleID,
            List<ResolveParser.TypeContext> actualTypes,
            ResolveParser.ModuleArgumentListContext actualArgs,
            FacilitySymbol instantiatingFacility, AnnotatedTree annotations,
            SymbolTable scopeRepo) {
        this.instantiatingFacility = instantiatingFacility;
        this.scopeRepo = scopeRepo;
        this.suppliedGenerics.addAll(actualTypes);
        this.annotations = annotations;

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
                int i;
                i = 0;
                /*  result =
                          new InstantiatedScope(originalScope,
                                  genericInstantiations, instantiatingFacility);*/
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

        Iterator<GenericSymbol> formalGenerics =
                moduleScope.getSymbolsOfType(GenericSymbol.class).iterator();
        Iterator<ResolveParser.TypeContext> actualGenerics =
                suppliedGenerics.iterator();

        if ( suppliedGenerics.size() != suppliedGenerics.size() ) {
            throw new RuntimeException("generic list sizes do not match");
        }

        while (formalGenerics.hasNext()) {
            result.put(formalGenerics.next().getName(),
                    annotations.progTypeValues.get(actualGenerics.next()));
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
