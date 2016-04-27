package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathSymbolTable;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.ModuleParameterization;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FacilitySymbol extends Symbol {

    @NotNull
    private SpecImplementationPairing type;
    @NotNull
    private MathSymbolTable scopeRepo;

    /**
     * A mapping from the rule contexts representing an module arg list
     * to all the various {@link ProgTypeSymbol}s that represent the actual
     * versions of formal (generic) type params.
     * <p>
     * Right now I'm really only especially concerned about these as they
     * factor into the searching process in {@link ModuleParameterization}.</p>
     */
    @NotNull
    private final ParseTreeProperty<List<ProgTypeSymbol>> actualGenerics;

    private final Map<ModuleParameterization, ModuleParameterization>
            enhancementImplementations = new HashMap<>();
    private final List<ModuleParameterization> enhancements =
            new ArrayList<>();

    public FacilitySymbol(
            @NotNull ResolveParser.FacilityDeclContext facility,
            @NotNull ModuleIdentifier moduleIdentifier,
            @NotNull ParseTreeProperty<List<ProgTypeSymbol>> actualGenerics,
            @NotNull MathSymbolTable scopeRepo) {
        super(facility.name.getText(), facility, moduleIdentifier);
        this.scopeRepo = scopeRepo;
        this.actualGenerics = actualGenerics;
        List<ProgTypeSymbol> specGenericArgs =
                actualGenerics.get(facility.specArgs);

        ModuleParameterization spec = new ModuleParameterization(
                new ModuleIdentifier(facility.spec),
                specGenericArgs==null ?
                        new ArrayList<>() : specGenericArgs, this, scopeRepo);

        ModuleParameterization impl = new ModuleParameterization(
                new ModuleIdentifier(facility.impl),
                new ArrayList<>(), this, scopeRepo);

        this.type = new SpecImplementationPairing(spec, impl);

        //These are realized by the concept realization
        /*for (EnhancementItem realizationEnhancement : facility
                .getEnhancements()) {

            spec =
                    new ModuleParameterization(new ModuleIdentifier(
                            realizationEnhancement.getNameToken().getNameToken()),
                            realizationEnhancement.getParams(), this,
                            mySourceRepository);

            myEnhancements.add(spec);
            myEnhancementRealizations.put(spec, realization);
        }*/

        //These are realized by individual extension implementations
        /*for (ResolveParser.ExtensionPairingContext extension :
                facility.extensionPairing()) {
            specGenericArgs = actualGenerics.get(extension.specArgs);
            spec = new ModuleParameterization(new ModuleIdentifier(extension.spec),
                    specGenericArgs == null ?
                            new ArrayList<>() : specGenericArgs,
                    this, scopeRepo);

            impl = new ModuleParameterization(
                    new ModuleIdentifier(extension.impl),
                    new ArrayList<>(), this, scopeRepo);
            enhancements.add(spec);
            enhancementImplementations.put(spec, impl);
        }*/
    }

    @NotNull
    public List<ModuleParameterization> getEnhancements() {
        return enhancements;
    }

    @NotNull
    public SpecImplementationPairing getFacility() {
        return type;
    }

    @NotNull
    @Override
    public String getSymbolDescription() {
        return "a facility";
    }

    @NotNull
    @Override
    public String toString() {
        return getName();
    }

    @NotNull
    @Override
    public FacilitySymbol toFacilitySymbol() {
        return this;
    }

    @NotNull
    @Override
    public FacilitySymbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        //TODO : This is probably wrong.  One of the parameters to a module
        //       used in the facility could be a generic, in which case it
        //       should be replaced with the corresponding concrete type--but
        //       how?
        return this;
    }

    public static class SpecImplementationPairing {

        @NotNull
        private final ModuleParameterization specification;
        @Nullable
        private final ModuleParameterization implementation;

        public SpecImplementationPairing(@NotNull ModuleParameterization spec) {
            this(spec, null);
        }

        public SpecImplementationPairing(@NotNull ModuleParameterization spec,
                                         @Nullable ModuleParameterization impl) {
            this.specification = spec;
            this.implementation = impl;
        }

        @NotNull
        public ModuleParameterization getSpecification() {
            return specification;
        }

        @Nullable
        public ModuleParameterization getImplementation() {
            return implementation;
        }
    }

}
