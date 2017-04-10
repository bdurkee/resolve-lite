package edu.clemson.resolve.semantics;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.semantics.programtype.ProgInvalidType;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.symbol.*;

import java.util.*;

public class ModuleParameterization {

    private final MathSymbolTable scopeRepo;
    private final ModuleIdentifier moduleIdentifier;

    private final List<ProgTypeSymbol> actualGenerics = new ArrayList<>();
    private final FacilitySymbol instantiatingFacility;

    public ModuleParameterization(@NotNull ModuleIdentifier moduleIdentifier,
                                  @NotNull List<ProgTypeSymbol> actualGenerics,
                                  @NotNull FacilitySymbol instantiatingFacility,
                                  @NotNull MathSymbolTable scopeRepo) {
        this.instantiatingFacility = instantiatingFacility;
        this.scopeRepo = scopeRepo;
        this.actualGenerics.addAll(actualGenerics);
        this.moduleIdentifier = moduleIdentifier;
    }

    @NotNull
    public Scope getScope(boolean instantiated) throws NoSuchModuleException {
        ModuleScopeBuilder originalScope = scopeRepo.getModuleScope(moduleIdentifier);
        Scope result = originalScope;
        result = scopeRepo.getModuleScope(moduleIdentifier);
        if (instantiated) {
            Map<String, ProgType> genericInstantiations =
                    getGenericInstantiations(originalScope, new ArrayList<>());
            result = new InstantiatedScope(originalScope,
                    genericInstantiations, instantiatingFacility);
        }
        return result;
    }

    /*private List<ModuleParameterSymbol> getFormalParameters(
            boolean instantiateGenerics)
            throws NoSuchModuleException {
        ModuleScopeBuilder s = scopeRepo.getModuleScope(moduleIdentifier);
        List<ModuleParameterSymbol> moduleParams =
                s.getSymbolsOfType(ModuleParameterSymbol.class);
        if (instantiateGenerics) {
            for (ModuleParameterSymbol moduleParam : )
        }
        return
    }*/

    //TODO: Annotate actualArguments as not null!
    private Map<String, ProgType> getGenericInstantiations(
            @NotNull ModuleScopeBuilder moduleScope,
            List<ResolveParser.ProgExpContext> actualArguments) {
        Map<String, ProgType> result = new HashMap<>();

        List<ModuleParameterSymbol> moduleParams =
                moduleScope.getSymbolsOfType(ModuleParameterSymbol.class);
        List<ProgParameterSymbol> formalGenerics = new ArrayList<>();

        //TODO :Here instead of building a map from String -> ProgType,
        //I want a map from ProgType->ProgType (specifically two progtypes representing generics), then
        //I can pull out the MathNamedType and do substitutions, etc
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
        if (formalGenerics.size() != actualGenerics.size()) {

            //we shouldn't have to do this in here I don't think. Can't really
            //give a nice error (no pointer to errMgr here), and we can't throw
            // an exception to be caught
            //in the populator -- unless of course adding yet another caught
            //exception to the signature of Scope.* methods sounds appealing...
            //which it certainly doesn't.
            //throw new RuntimeException("generic list sizes do not match");
        }
        Iterator<ProgTypeSymbol> suppliedGenericIter = actualGenerics.iterator();
        for (ProgParameterSymbol formalGeneric : formalGenerics) {
            ProgType suppliedGeneric = null;
            if (suppliedGenericIter.hasNext()) {
                suppliedGeneric = suppliedGenericIter.next().getProgramType();
            }
            else {
                suppliedGeneric = ProgInvalidType.getInstance(formalGeneric.getDeclaredType().getTypeGraph());
            }
            result.put(formalGeneric.getName(), suppliedGeneric);
        }
        return result;
    }

    @NotNull
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }
}
