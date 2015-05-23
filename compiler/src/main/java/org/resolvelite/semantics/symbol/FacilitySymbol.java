package org.resolvelite.semantics.symbol;

import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.ModuleParameterization;
import org.resolvelite.semantics.SpecImplementationPairing;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.programtype.PTType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FacilitySymbol extends Symbol {

    private final SpecImplementationPairing type;
    private final SymbolTable scopeRepo;
    private final List<ProgTypeSymbol> actualGenerics = new ArrayList<>();

    public FacilitySymbol(ResolveParser.FacilityDeclContext facility,
            String moduleID, List<ProgTypeSymbol> actualGenerics,
            SymbolTable scopeRepo) {
        super(facility.name.getText(), facility, moduleID);
        this.scopeRepo = scopeRepo;
        this.actualGenerics.addAll(actualGenerics);
        ModuleParameterization spec =
                new ModuleParameterization(facility.spec.getText(),
                        actualGenerics, facility.specArgs, this, scopeRepo);

        ModuleParameterization impl = null;

        if ( facility.impl != null ) {
            List<ResolveParser.ModuleArgumentContext> actualArgs =
                    facility.implArgs != null ? facility.implArgs
                            .moduleArgument() : new ArrayList<>();
            impl =
                    new ModuleParameterization(facility.impl.getText(),
                            actualGenerics, facility.implArgs, this, scopeRepo);
        }
        this.type = new SpecImplementationPairing(spec, impl);

        //These are realized by the concept realization
        /*for (EnhancementItem realizationEnhancement : facility
                .getEnhancements()) {

            spec =
                    new ModuleParameterization(new ModuleIdentifier(
                            realizationEnhancement.getName().getName()),
                            realizationEnhancement.getParams(), this,
                            mySourceRepository);

            myEnhancements.add(spec);
            myEnhancementRealizations.put(spec, realization);
        }

        //These are realized by individual enhancement realizations
        for (EnhancementBodyItem enhancement : facility.getEnhancementBodies()) {

            spec =
                    new ModuleParameterization(new ModuleIdentifier(enhancement
                            .getName().getName()), enhancement.getParams(),
                            this, mySourceRepository);

            realization =
                    new ModuleParameterization(new ModuleIdentifier(enhancement
                            .getBodyName().getName()), enhancement
                            .getBodyParams(), this, mySourceRepository);

            myEnhancements.add(spec);
            myEnhancementRealizations.put(spec, realization);
        }*/
    }

    public SpecImplementationPairing getFacility() {
        return type;
    }

    @Override public String getSymbolDescription() {
        return "a facility";
    }

    @Override public String toString() {
        return getName();
    }

    @Override public FacilitySymbol toFacilitySymbol() {
        return this;
    }

    @Override public FacilitySymbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        //TODO : This is probably wrong.  One of the parameters to a module
        //       used in the facility could be a generic, in which case it
        //       should be replaced with the corresponding concrete type--but
        //       how?
        return this;
    }
}
