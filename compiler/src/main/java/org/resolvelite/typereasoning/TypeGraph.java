package org.resolvelite.typereasoning;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.*;

import java.util.List;
import java.util.NoSuchElementException;

public class TypeGraph {
    public final MTInvalid INVALID = MTInvalid.getInstance(this);
    public final MTType ELEMENT = new MTProper(this, "Element");
    public final MTProper ENTITY = new MTProper(this, "Entity");

    public final MTProper CLS = new MTProper(this, null, true, "Cls");
    public final MTProper SSET = new MTProper(this, CLS, true, "SSet");
    public final MTProper VOID = new MTProper(this, SSET, false, "Void");

    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");
    public final MTProper Z = new MTProper(this, SSET, false, "Z");

    public final MTProper EMPTY_SET = new MTProper(this, SSET, false,
            "Empty_Set");

    public final MTFunction POWERSET = //
            new MTFunction.MTFunctionBuilder(this, POWERSET_APPLICATION, SSET) //
                    .paramTypes(SSET) //
                    .elementsRestrict(true).build();

    private final static FunctionApplicationFactory POWERSET_APPLICATION =
            new PowersetApplicationFactory();

    private static class PowersetApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String refName, List<MTType> args) {
            return new MTPowersetApplication(g, args.get(0));
        }
    }

    public boolean isKnownToBeIn(MTType value, MTType expected) {
        boolean result;

        result = (value != CLS) && (value != ENTITY)
                        && isSubtype(value.getType(), expected);
        return result;
    }

    public boolean isKnownToBeIn(PExp value, MTType expected) {
        return false;
    }

    public boolean isSubtype(MTType subtype, MTType supertype) {
        boolean result;

        try {
            result =
                    supertype == ENTITY || supertype == CLS
                           // || myEstablishedSubtypes.contains(r)
                            || subtype.equals(supertype);
                           // || subtype.isSyntacticSubtypeOf(supertype);
        }
        catch (NoSuchElementException nsee) {
            //Syntactic subtype checker freaks out (rightly) if there are
            //free variables in the expression, but the next check will deal
            //correctly with them.
            result = false;
        }
        return result;
    }
}
