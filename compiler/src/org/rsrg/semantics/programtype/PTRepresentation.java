package org.rsrg.semantics.programtype;

import org.antlr.v4.runtime.misc.Nullable;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.NoneProvidedException;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.ProgReprTypeSymbol;
import org.rsrg.semantics.symbol.ProgTypeModelSymbol;
import org.rsrg.semantics.TypeGraph;

import java.util.Map;

/**
 * A {@code PTRepresentation} wraps an existing {@link PTType PTType} with
 * additional information about a {@link PTFamily PTFamily} this type
 * represents. An instance of {@code PTRepresentation} is thus a special
 * case of its wrapped type that happens to be functioning as a representation
 * type.
 */
public class PTRepresentation extends PTNamed {

    private final PTType baseType;
    private final String name;

    /**
     * This will be {@code null} for standalone representations (i.e. those that
     * would appear in the context of a facility module.
     */
    private final ProgTypeModelSymbol family;
    private ProgReprTypeSymbol repr;

    public PTRepresentation(TypeGraph g, PTType baseType, String name,
            @Nullable ProgTypeModelSymbol family, String enclosingModuleID) {
        super(g, name, g.getTrueExp(), enclosingModuleID);
        this.name = name;
        this.baseType = baseType;
        this.family = family;
    }

    public void setReprTypeSymbol(ProgReprTypeSymbol t) {
        this.repr = t;
    }

    public ProgReprTypeSymbol getReprTypeSymbol() {
        return repr;
    }

    public PTType getBaseType() {
        return baseType;
    }

    public ProgTypeModelSymbol getFamily() throws NoneProvidedException {
        if ( family == null ) {
            throw new NoneProvidedException();
        }
        return family;
    }

    public String getExemplarName() {
        if ( family != null ) {
            return family.getExemplar().getName();
        }
        return name.substring(0, 1);
    }

    @Override public MTType toMath() {
        return baseType.toMath();
    }

    @Override public boolean isAggregateType() {
        return baseType.isAggregateType();
    }

    @Override public boolean acceptableFor(PTType t) {
        boolean result = super.acceptableFor(t);
        if ( !result && family != null ) {
            result = family.getProgramType().acceptableFor(t);
        }
        return result;
    }

    @Override public PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        throw new UnsupportedOperationException(this.getClass() + " cannot "
                + "be instantiated.");
    }

    @Override public String toString() {
        return name + " as " + baseType;
    }
}
