package org.rsrg.semantics.symbol;

import edu.clemson.resolve.parser.Resolve;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.rsrg.semantics.ModuleParameterization;
import org.rsrg.semantics.SpecImplementationPairing;
import org.rsrg.semantics.SymbolTable;
import org.rsrg.semantics.programtype.PTType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FacilitySymbol extends Symbol {

    private final SpecImplementationPairing type;
    private final SymbolTable scopeRepo;

    private final ParseTreeProperty<List<ProgTypeSymbol>> genericsPerFacility;

    private final Map<ModuleParameterization, ModuleParameterization>
            enhancementImplementations = new HashMap<>();
    private final List<ModuleParameterization> enhancements = new ArrayList<>();

    public FacilitySymbol(Resolve.FacilityDeclContext facility,
            String moduleID,
            @NotNull ParseTreeProperty<List<ProgTypeSymbol>> actualGenerics,
            SymbolTable scopeRepo) {
        super(facility.name.getText(), facility, moduleID);
        this.scopeRepo = scopeRepo;
        this.genericsPerFacility = actualGenerics;
        ModuleParameterization spec =
                new ModuleParameterization(facility.spec.getText(),
                        genericsPerFacility.get(facility), facility.specArgs,
                        this, scopeRepo);

        ModuleParameterization impl = null;

        List<Resolve.ModuleArgumentContext> actualArgs =
                facility.implArgs != null ? facility.implArgs
                        .moduleArgument() : new ArrayList<>();
        impl = new ModuleParameterization(facility.impl.getText(),
                        new ArrayList<>(), facility.implArgs, this, scopeRepo);

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
        }*/

        //These are realized by individual enhancement realizations
        for (Resolve.EnhancementPairDeclContext enhancement :
                facility.enhancementPairDecl()) {

            spec = new ModuleParameterization(enhancement.spec.getText(),
                            genericsPerFacility.get(enhancement),
                            enhancement.specArgs, this, scopeRepo);

            impl = new ModuleParameterization(enhancement.impl.getText(),
                        new ArrayList<>(), enhancement.implArgs,
                            this, scopeRepo);
            enhancements.add(spec);
            enhancementImplementations.put(spec, impl);
        }
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
