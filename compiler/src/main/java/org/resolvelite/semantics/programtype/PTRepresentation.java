package org.resolvelite.semantics.programtype;

import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.symbol.ProgTypeDefinitionSymbol;
import org.resolvelite.typereasoning.TypeGraph;

/**
 * <p>
 * A <code>PTRepresentation</code> wraps an existing {@link PTType PTType} with
 * additional information about a {@link PTFamily PTFamily} this type
 * represents. An instance of <code>PTRepresentation</code> is thus a special
 * case of its wrapped type that happens to be functioning as a representation
 * type.
 * </p>
 */
public class PTRepresentation extends PTType {

    private final PTType baseType;
    private final ProgTypeDefinitionSymbol family;

    public PTRepresentation(TypeGraph g, PTType baseType,
            ProgTypeDefinitionSymbol family) {
        super(g);
        this.baseType = baseType;
        this.family = family;
    }

    public PTType getBaseType() {
        return baseType;
    }

    public ProgTypeDefinitionSymbol getFamily() {
        return family;
    }

    @Override public MTType toMath() {
        return baseType.toMath();
    }

    /*@Override
    public PTType instantiateGenerics(
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
        return family.getName() + " as " + baseType;
    }
}
