package org.rsrg.semantics;

import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.ProgTypeSymbol;

import java.util.*;

public class ModuleParameterization {

    @NotNull private final MathSymbolTable scopeRepo;
    @NotNull private final ModuleIdentifier moduleIdentifier;

    @NotNull private final List<ResolveParser.ModuleArgumentContext> arguments =
            new ArrayList<>();
    @NotNull private final List<ProgTypeSymbol> actualGenerics = new ArrayList<>();
    @NotNull private final FacilitySymbol instantiatingFacility;

    public ModuleParameterization(@NotNull ModuleIdentifier moduleIdentifier,
                                  @NotNull List<ProgTypeSymbol> actualGenerics,
                                  @Nullable ResolveParser.ModuleArgumentListContext actualArgListNode,
                                  @NotNull FacilitySymbol instantiatingFacility,
                                  @NotNull MathSymbolTable scopeRepo) {
        this.instantiatingFacility = instantiatingFacility;
        this.scopeRepo = scopeRepo;
        this.actualGenerics.addAll(actualGenerics);

        if ( actualArgListNode != null ) {
            arguments.addAll(actualArgListNode.moduleArgument());
        }
        this.moduleIdentifier = moduleIdentifier;
    }

    @NotNull public Scope getScope(boolean instantiated)
            throws NoSuchModuleException {
        Scope result;
        result = scopeRepo.getModuleScope(moduleIdentifier);
        if ( instantiated ) {
            Map<String, PTType> genericInstantiations;
           // genericInstantiations =
           //         getGenericInstantiations(originalScope, null);
            //result =
           //         new InstantiatedScope(originalScope,
            //                genericInstantiations, instantiatingFacility);
        }
        return result;
    }

    /*private Map<String, PTType> getGenericInstantiations(
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
            result.put(formalGenericIter.next().getNameToken(), suppliedGenericIter
                    .next().getProgramType());
        }
        return result;
    }*/

    @NotNull public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    @NotNull public List<ResolveParser.ModuleArgumentContext> getArguments() {
        return arguments;
    }
}
