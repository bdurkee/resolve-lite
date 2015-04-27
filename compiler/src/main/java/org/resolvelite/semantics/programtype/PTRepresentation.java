package org.resolvelite.semantics.programtype;

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
public class PTRepresentation extends PTType {

    private final PTType baseType;
    private final String name;
    /**
     * This will be {@code null} for standalone representations (i.e. those that
     * would appear in the context of a facility module.
     */
    private final ProgTypeModelSymbol family;

    public PTRepresentation(TypeGraph g, PTType baseType, String name,
            ProgTypeModelSymbol family) {
        super(g);
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

    @Override public MTType toMath() {
        return baseType.toMath();
    }

    @Override public boolean isAggregateType() {
        return baseType.isAggregateType();
    }

    /*@Override
    public PTType instantiatseGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility) {

        throw new UnsupportedOperationException(this.getClass() + " cannot "
                + "be instantiated.");
    }

    @Override
    public boolean acceptableFor(PTType t) {
        boolean result = super.acceptableFor(t);

        if (!result) {
            result = family.getProgramType().acceptableFor(t);
        }

        return result;
    }*/

    @Override public String toString() {
        return name + " as " + baseType;
    }
}
