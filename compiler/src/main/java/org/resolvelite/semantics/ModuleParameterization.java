package org.resolvelite.semantics;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.symbol.FacilitySymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModuleParameterization {

    private final SymbolTable scopeRepo;
    private final String moduleID;

    private final List<ResolveParser.ModuleArgumentContext> arguments =
            new ArrayList<>();

    private final FacilitySymbol instantiatingFacility;

    public ModuleParameterization(String moduleID,
            List<ResolveParser.ModuleArgumentContext> args,
            FacilitySymbol instantiatingFacility, SymbolTable scopeRepo) {
        this.instantiatingFacility = instantiatingFacility;
        this.scopeRepo = scopeRepo;

        if ( args != null ) {
            this.arguments.addAll(args);
        }
        this.moduleID = moduleID;
    }

    public ModuleParameterization(String moduleID,
            ResolveParser.ModuleArgumentListContext args,
            FacilitySymbol instantiatingFacility, SymbolTable scopeRepo) {
        this(moduleID, args != null ? args.moduleArgument()
                : new ArrayList<ResolveParser.ModuleArgumentContext>(),
                instantiatingFacility, scopeRepo);
    }

    public Scope getScope(boolean instantiated) {
        Scope result;
        try {
            ModuleScopeBuilder originalScope =
                    scopeRepo.getModuleScope(moduleID);
            result = originalScope;

            /*if (instantiated) {
                Map<String, PTType> genericInstantiations;

                genericInstantiations =
                        getGenericInstantiations(originalScope,
                                myParameters);
                result =
                        new InstantiatedScope(originalScope,
                                genericInstantiations, myInstantiatingFacility);
            }*/
        }
        catch (NoSuchSymbolException nsse) {
            //Shouldn't be possible--we'd have caught it by now
            throw new RuntimeException(nsse);
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
