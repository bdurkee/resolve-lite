package org.rsrg.semantics;

import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.symbol.*;

import java.util.*;

public class ModuleParameterization {

    @NotNull private final MathSymbolTable scopeRepo;
    @NotNull private final ModuleIdentifier moduleIdentifier;

    @NotNull private final List<ResolveParser.ProgExpContext> arguments =
            new ArrayList<>();

    //OK, so I guess we need both actual symbols + The PTTypes since we (obviously)
    //won't get symbols for things like 1, 0, or other literals or exprs...
    @NotNull private final List<Symbol> actualSymbols = new ArrayList<>();
    @NotNull private final FacilitySymbol instantiatingFacility;

    public ModuleParameterization(@NotNull ModuleIdentifier moduleIdentifier,
                                  @NotNull List<Symbol> actualSymbols,
                                  @NotNull FacilitySymbol instantiatingFacility,
                                  @NotNull MathSymbolTable scopeRepo) {
        this.instantiatingFacility = instantiatingFacility;
        this.scopeRepo = scopeRepo;
        this.actualSymbols.addAll(actualSymbols);
        this.moduleIdentifier = moduleIdentifier;
    }

    @NotNull public Scope getScope(boolean instantiated)
            throws NoSuchModuleException {
        ModuleScopeBuilder originalScope =
                scopeRepo.getModuleScope(moduleIdentifier);
        Scope result = originalScope;
        result = scopeRepo.getModuleScope(moduleIdentifier);
        if ( instantiated ) {
            Map<String, PTType> genericInstantiations;
            genericInstantiations = getGenericInstantiations(originalScope, null);
            result = new InstantiatedScope(originalScope,
                        genericInstantiations, instantiatingFacility);
        }
        return result;
    }

    private Map<String, PTType> getGenericInstantiations(
            ModuleScopeBuilder moduleScope,
            List<ResolveParser.ProgExpContext> actualArguments) {
        Map<String, PTType> result = new HashMap<>();

        List<ModuleParameterSymbol> moduleParams =
                moduleScope.getSymbolsOfType(ModuleParameterSymbol.class);
        List<ProgParameterSymbol> formalGenerics = new ArrayList<>();

        for (ModuleParameterSymbol param : moduleParams) {
            try {
                ProgParameterSymbol p = param.toProgParameterSymbol();
                if (p.getMode() == ProgParameterSymbol.ParameterMode.TYPE) {
                    formalGenerics.add(p);
                }
            } catch (UnexpectedSymbolException e) {
                //no problem, we wont add it.
            }
        }
       /* if ( formalGenerics.size() != actualGenerics.size() ) {
            throw new RuntimeException("generic list sizes do not match");
        }
        Iterator<ProgTypeSymbol> suppliedGenericIter =
                actualGenerics.iterator();
        Iterator<ProgParameterSymbol> formalGenericIter = formalGenerics.iterator();
        while (formalGenericIter.hasNext()) {
            result.put(formalGenericIter.next().getName(), suppliedGenericIter
                    .next().getProgramType());
        }*/
        return result;
    }

    @NotNull public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    @NotNull public List<ResolveParser.ProgExpContext> getArguments() {
        return arguments;
    }
}
