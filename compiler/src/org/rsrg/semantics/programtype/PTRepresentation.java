package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MathType;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.ProgReprTypeSymbol;
import org.rsrg.semantics.symbol.TypeModelSymbol;
import org.rsrg.semantics.DumbTypeGraph;

import java.util.Map;
import java.util.NoSuchElementException;

/** A {@code PTRepresentation} wraps an existing {@link ProgType ProgType} with
 *  additional information about a {@link PTFamily PTFamily} this type
 *  represents. An instance of {@code PTRepresentation} is thus a special
 *  case of its wrapped type that happens to be functioning as a representation
 *  type.
 */
public class PTRepresentation extends PTNamed {

    @NotNull private final ProgType baseType;
    @NotNull private final String name;

    /** This will be {@code null} for standalone representations (i.e. those that
     *  would appear in the context of a facility module.
     */
    @Nullable private final TypeModelSymbol family;
    @Nullable private ProgReprTypeSymbol repr;

    public PTRepresentation(@NotNull DumbTypeGraph g, @NotNull ProgType baseType,
                            @NotNull String name,
                            @Nullable TypeModelSymbol family,
                            @NotNull ModuleIdentifier moduleIdentifier) {
        super(g, name, g.getTrueExp(), moduleIdentifier);
        this.name = name;
        this.baseType = baseType;
        this.family = family;
    }
    
    public void setReprTypeSymbol(@Nullable ProgReprTypeSymbol t) {
        this.repr = t;
    }

    @Nullable public ProgReprTypeSymbol getReprTypeSymbol() {
        return repr;
    }

    @NotNull public ProgType getBaseType() {
        return baseType;
    }

    @Nullable public TypeModelSymbol getFamily() throws NoSuchElementException {
        if ( family == null ) {
            throw new NoSuchElementException("no family found for this " +
                    "representation: " + toString());
        }
        return family;
    }

    @NotNull public String getExemplarName() {
        if ( family != null ) {
            return family.getExemplar().getName();
        }
        return name.substring(0, 1);
    }

    @NotNull @Override public MathType toMath() {
        return baseType.toMath();
    }

    @Override public boolean isAggregateType() {
        return baseType.isAggregateType();
    }

    @Override public boolean acceptableFor(@NotNull ProgType t) {
        boolean result = super.acceptableFor(t);
        if ( !result && family != null ) {
            result = family.getProgramType().acceptableFor(t);
        }
        return result;
    }

    @NotNull @Override public ProgType instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        throw new UnsupportedOperationException(this.getClass() + " cannot "
                + "be instantiated.");
    }

    @Override public String toString() {
        return name + " as " + baseType;
    }
}
