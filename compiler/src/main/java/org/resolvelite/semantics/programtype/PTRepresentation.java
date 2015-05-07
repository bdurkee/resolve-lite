package org.resolvelite.semantics.programtype;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.symbol.ProgTypeModelSymbol;
import org.resolvelite.typereasoning.TypeGraph;

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

    public PTRepresentation(TypeGraph g, PTType baseType, String name,
            @Nullable ProgTypeModelSymbol family, PExp initRequires,
            PExp initEnsures, PExp finalRequires, PExp finalEnsures,
            String enclosingModuleID) {
        super(g, name, initRequires, initEnsures, finalRequires, finalEnsures,
                enclosingModuleID);
        this.name = name;
        this.baseType = baseType;
        this.family = family;
    }

    public PTType getBaseType() {
        return baseType;
    }

    public ProgTypeModelSymbol getFamily() {
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

    /*@Override public PTType instantiatseGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility) {

        throw new UnsupportedOperationException(this.getClass() + " cannot "
                + "be instantiated.");
    }*/

    @Override public boolean acceptableFor(PTType t) {
        boolean result = super.acceptableFor(t);
        if ( !result && family != null ) {
            result = family.getProgramType().acceptableFor(t);
        }
        return result;
    }

    @Override public String toString() {
        return name + " as " + baseType;
    }
}
