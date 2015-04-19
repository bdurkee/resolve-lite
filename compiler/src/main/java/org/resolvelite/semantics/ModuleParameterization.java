package org.resolvelite.semantics;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.symbol.FacilitySymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public String getModuleID() {
        return moduleID;
    }

    public List<ResolveParser.ModuleArgumentContext> getArguments() {
        return arguments;
    }
}
