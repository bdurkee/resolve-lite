package org.resolvelite.semantics.symbol;

import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.ModuleParameterization;
import org.resolvelite.semantics.SpecImplementationPairing;
import org.resolvelite.semantics.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class FacilitySymbol extends Symbol {

    private final SpecImplementationPairing type;
    private final SymbolTable scopeRepo;
    private final AnnotatedTree annotations;

    public FacilitySymbol(ResolveParser.FacilityDeclContext facility,
            String moduleID, AnnotatedTree annotations, SymbolTable scopeRepo) {
        super(facility.name.getText(), facility, moduleID);
        this.scopeRepo = scopeRepo;
        this.annotations = annotations;
        ModuleParameterization spec =
                new ModuleParameterization(facility.spec.getText(),
                        facility.type(), facility.specArgs, this, annotations,
                        scopeRepo);

        ModuleParameterization impl = null;

        if ( facility.impl != null ) {
            List<ResolveParser.ModuleArgumentContext> actualArgs =
                    facility.implArgs != null ? facility.implArgs
                            .moduleArgument() : new ArrayList<>();
            impl =
                    new ModuleParameterization(facility.impl.getText(),
                            facility.type(), facility.implArgs, this,
                            annotations, scopeRepo);
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
