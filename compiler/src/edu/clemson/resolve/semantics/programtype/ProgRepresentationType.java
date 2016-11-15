package edu.clemson.resolve.semantics.programtype;

import edu.clemson.resolve.proving.absyn.PSelector;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.semantics.symbol.ProgReprTypeSymbol;
import edu.clemson.resolve.semantics.symbol.TypeModelSymbol;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A {@code ProgRepresentation} wraps an existing {@link ProgType ProgType} with additional information about a
 * {@link ProgFamilyType ProgFamilyType} this type represents. An instance of {@code PTRepresentation} is thus a
 * special case of its wrapped type that happens to be functioning as a representation type.
 */

//TODO: Have a ProgFacilityRepresentationType that disallows correspondences..
    //this will clarify some confusion during vcgen...
public class ProgRepresentationType extends ProgNamedType {

    private final ProgType baseType;
    private final String name;

    /** This will be {@code null} for standalone representations (i.e. those that would appear in the context of a facility module. */
    private final TypeModelSymbol family;
    private ProgReprTypeSymbol repr;

    public ProgRepresentationType(@NotNull DumbMathClssftnHandler g,
                                  @NotNull ProgType baseType,
                                  @NotNull String name,
                                  @Nullable TypeModelSymbol family,
                                  @NotNull ModuleIdentifier moduleIdentifier) {
        super(g, name, family != null ? family.getProgramType().getInitializationEnsures() : g.getTrueExp(),
                moduleIdentifier);
        this.name = name;
        this.baseType = baseType;
        this.family = family;
    }

    public void setReprTypeSymbol(@Nullable ProgReprTypeSymbol t) {
        this.repr = t;
    }

    @Nullable
    public ProgReprTypeSymbol getReprTypeSymbol() {
        return repr;
    }

    @NotNull
    public ProgType getBaseType() {
        return baseType;
    }

    @Nullable
    public TypeModelSymbol getFamily() throws NoSuchElementException {
        if (family == null) {
            throw new NoSuchElementException("no family found for this representation: " + toString());
        }
        return family;
    }

    @NotNull
    public PSelector getConceptualExemplar() {
        return getConceptualExemplar(false);
    }

    @NotNull
    public PSelector getConceptualExemplar(boolean incoming) {
        return new PSelector(
                new PSymbol.PSymbolBuilder("conc").mathClssfctn(g.BOOLEAN)
                        .incoming(incoming).build(),
                new PSymbol.PSymbolBuilder(getExemplarName())
                        .mathClssfctn(getBaseType().toMath()).build());
    }

    @NotNull
    public String getExemplarName() {
        if (family != null) {
            return family.getExemplar().getName();
        }
        return name.substring(0, 1);
    }

    @NotNull
    @Override
    public MathClssftn toMath() {
        if (baseType == null) return g.INVALID;
        return baseType.toMath();
    }

    @Override
    public boolean isAggregateType() {
        return baseType.isAggregateType();
    }

    @Override
    public boolean acceptableFor(@NotNull ProgType t) {
        boolean result = super.acceptableFor(t);
        if (!result && family != null) {
            result = family.getProgramType().acceptableFor(t);
        }
        return result;
    }

    @NotNull
    @Override
    public ProgType instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        throw new UnsupportedOperationException(this.getClass() + " cannot be instantiated.");
    }

    @Override
    public String toString() {
        return name + " as " + baseType;
    }
}
