package org.resolvelite.typereasoning;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.*;

import java.util.List;
import java.util.NoSuchElementException;

public class TypeGraph {
    public final MTInvalid INVALID = MTInvalid.getInstance(this);
    public final MTType ELEMENT = new MTProper(this, "Element");
    public final MTProper ENTITY = new MTProper(this, "Entity");

    public final MTProper CLS = new MTProper(this, null, true, "Cls");
    public final MTProper SSET = new MTProper(this, CLS, false, "SSet");
    public final MTProper VOID = new MTProper(this, SSET, false, "Void");
    public final MTProper CARD = new MTProper(this, CLS, false, "Card");

    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");
    public final MTProper Z = new MTProper(this, SSET, false, "Z");

    public final MTProper EMPTY_SET = new MTProper(this, SSET, false,
            "Empty_Set");

    private final static FunctionApplicationFactory POWERSET_APPLICATION =
            new PowersetApplicationFactory();
    private final static FunctionApplicationFactory UNION_APPLICATION =
            new UnionApplicationFactory();

    public final MTFunction POWERSET = //
            new MTFunction.MTFunctionBuilder(this, POWERSET_APPLICATION, SSET) //
                    .paramTypes(SSET) //
                    .elementsRestrict(true).build();

    public final MTFunction UNION = new MTFunction.MTFunctionBuilder(this,
            UNION_APPLICATION, SSET).paramTypes(SSET, SSET) //
            .build();

    private static class PowersetApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String refName, List<MTType> args) {
            return new MTPowersetApplication(g, args.get(0));
        }
    }

    private static class UnionApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String calledAsName, List<MTType> arguments) {
            return new MTUnion(g, arguments);
        }
    }

    public boolean isKnownToBeIn(MTType value, MTType expected) {
        boolean result;

        result =
                (value != CLS) && (value != ENTITY)
                        && isSubtype(value.getType(), expected);
        return result;
    }

    public boolean isKnownToBeIn(PExp value, MTType expected) {
        boolean result;
        try {
            PExp conditions = getValidTypeConditions(value, expected);
            result = conditions.isLiteralTrue();
        }
        catch (TypeMismatchException e) {
            result = false;
        }
        return result;
    }

    public PExp getValidTypeConditions(PExp value, MTType expected)
            throws TypeMismatchException {
        PExp result;
        MTType valueTypeValue = value.getMathTypeValue();

        if ( expected == ENTITY && valueTypeValue != CLS
                && valueTypeValue != ENTITY ) {
            result = getTrueExp();
        }
        else if ( valueTypeValue == CLS || valueTypeValue == ENTITY ) {
            //Cls and Entity aren't in anything (expect hyper-whatever, which we aren't concerned with)
            throw TypeMismatchException.INSTANCE;
        }
        else {
            result = getValidTypeConditions(valueTypeValue, expected);
        }

        return result;
    }

    private PExp getValidTypeConditions(MTType value, MTType expected)
            throws TypeMismatchException {
        PExp result = getFalseExp();

        if ( expected == SSET ) {
            //Every MTType is in SSet except for Entity and Cls
            result = getTrueExp();
        }

        /*else if (expected instanceof MTPowertypeApplication) {
            if (value.equals(EMPTY_SET)) {
                //The empty set is in all powertypes
                result = getTrueVarExp();
            }
            else {
                //If "expected" happens to be Power(t) for some t, we can
                //"demote" value to an INSTANCE of itself (provided it is not
                //the empty set), and expected to just t
                MTPowertypeApplication expectedAsPowertypeApplication =
                        (MTPowertypeApplication) expected;

                DummyExp memberOfValue = new DummyExp(value);

                if (isKnownToBeIn(memberOfValue, expectedAsPowertypeApplication
                        .getArgument(0))) {

                    result = getTrueVarExp();
                }
            }
        }*/

        //If we've already established it statically, no need for further work
        if ( !result.isLiteralTrue() ) {
            throw new UnsupportedOperationException("Cannot statically "
                    + "establish math subtype.");
        }
        return result;

    }

    public boolean isSubtype(MTType subtype, MTType supertype) {
        boolean result;

        try {
            result = supertype == ENTITY || supertype == CLS ||
                    supertype == SSET
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

    public final PSymbol getTrueExp() {
        return new PSymbolBuilder("true").literal(true).mathType(BOOLEAN)
                .build();
    }

    public final PSymbol getFalseExp() {
        return new PSymbolBuilder("false").literal(true).mathType(BOOLEAN)
                .build();
    }
}
