package org.resolvelite.semantics.symbol;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.ModuleParameterization;
import org.resolvelite.semantics.SpecImplementationPairing;
import org.resolvelite.semantics.SymbolTable;

public class FacilitySymbol extends Symbol {

    private final SpecImplementationPairing type;
    private final SymbolTable scopeRepo;

    public FacilitySymbol(ResolveParser.FacilityDeclContext facility,
            String moduleID, SymbolTable scopeRepo) {
        super(facility.name.getText(), facility, moduleID);
        this.scopeRepo = scopeRepo;
        ModuleParameterization spec =
                new ModuleParameterization(facility.spec.getText(),
                        facility.specArgs, this, scopeRepo);

        ModuleParameterization impl = null;
        if ( facility.impl != null ) {
            impl =
                    new ModuleParameterization(facility.impl.getText(),
                            facility.implArgs, this, scopeRepo);
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
}
