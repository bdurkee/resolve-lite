package org.rsrg.semantics;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PApply.DisplayStyle;
import edu.clemson.resolve.proving.absyn.PApply.PApplyBuilder;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;

import static org.rsrg.semantics.MTFunction.*;

public class TypeGraph {

    public final MTInvalid INVALID = MTInvalid.getInstance(this);
    public final MTType ELEMENT = new MTProper(this, "Element");
    public final MTProper ENTITY = new MTProper(this, "Entity");

    public final MTProper CLS = new MTProper(this, null, true, "Cls");
    public final MTProper SSET = new MTProper(this, CLS, true, "SSet");
    public final MTProper SSTR = new MTProper(this, SSET, true, "SStr");
    public final MTProper EMPTY_STRING = new MTProper(this, SSET, true,
            "Empty_String");

    public final MTProper VOID = new MTProper(this, SSET, false, "Void");

    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");
    public final MTProper Z = new MTProper(this, SSET, true, "Z");
    public final MTProper NAT = new MTProper(this, SSET, true, "N");

    public final MTProper BASE_POINT = new MTProper(this, SSET, false,
            "Base_Point");
    public final MTProper EMPTY_SET = new MTProper(this, SSET, false,
            "Empty_Set");

    private final static FunctionApplicationFactory POWERSET_APPLICATION =
            new PowersetApplicationFactory();
    private final static FunctionApplicationFactory FUNCTION_CONSTRUCTOR_APPLICATION =
            new FunctionConstructorApplicationFactory();
    private final static FunctionApplicationFactory CARTESIAN_PRODUCT_APPLICATION =
            new CartesianProductApplicationFactory();
    public final MTFunction FUNCTION = new MTFunctionBuilder(this,
            FUNCTION_CONSTRUCTOR_APPLICATION, SSET).paramTypes(SSET, SSET)
            .build();
    public final MTFunction CROSS =
            new MTFunctionBuilder(this,
                    CARTESIAN_PRODUCT_APPLICATION, CLS)
                    .paramTypes(CLS, CLS).build();

    public final MTFunction POWERSET =
            new MTFunctionBuilder(this, POWERSET_APPLICATION, SSET)
                    .paramTypes(SSET)
                    .elementsRestrict(true).build();

    public final MTFunction RELATIONAL_FUNCTION =
            new MTFunctionBuilder(this, BOOLEAN)
                    .paramTypes(ENTITY, ENTITY)
                    .build();

    /** A function where the everything is boolean: B * B -> B */
    public final MTFunction BOOLEAN_FUNCTION =
            new MTFunctionBuilder(this, BOOLEAN)
                    .paramTypes(BOOLEAN, BOOLEAN)
                    .build();

    private static class PowersetApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String refName, List<MTType> args) {
            return new MTPowersetApplication(g, args.get(0));
        }
    }

    private static class FunctionConstructorApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String calledAsName, List<MTType> arguments) {
            return new MTFunctionBuilder(g, arguments.get(1))
                    .paramTypes(arguments.get(0)).build();
        }
    }

    private static class CartesianProductApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override
        public MTType buildFunctionApplication(TypeGraph g, MTFunction f,
                               String calledAsName, List<MTType> arguments) {
            return new MTCartesian(g,
                    new MTCartesian.Element(arguments.get(0)),
                    new MTCartesian.Element(arguments.get(1)));
        }
    }

    public TypeGraph() {
    }

    @NotNull public PExp formConjuncts(PExp... e) {
        return formConjuncts(Arrays.asList(e));
    }

    @NotNull public PExp formConjuncts(List<PExp> e) {
        if ( e == null ) {
            throw new IllegalArgumentException("can't conjunct a null list");
        }
        if ( e.isEmpty() ) return null;
        Iterator<PExp> segsIter = e.iterator();
        PExp result = segsIter.next();
        if ( e.size() == 1 ) {
            return e.get(0);
        }
        while (segsIter.hasNext()) {
            result = formConjunct(result, segsIter.next());
        }
        return result;
    }

    @NotNull public PApply formConjunct(@NotNull PExp left, @NotNull PExp right) {
        PExp functionPortion = new PSymbolBuilder("and")
                .mathType(BOOLEAN_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull public PApply formDisjunct(PExp left, PExp right) {
        PExp functionPortion = new PSymbolBuilder("or")
                .mathType(BOOLEAN_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull public final PSymbol getTrueExp() {
        return new PSymbolBuilder("true").mathType(BOOLEAN).literal(true)
                .build();
    }

    @NotNull public final PSymbol getFalseExp() {
        return new PSymbolBuilder("false").mathType(BOOLEAN).literal(true)
                .build();
    }

    @NotNull public final PApply formEquals(PExp left, PExp right) {
        PExp functionPortion = new PSymbolBuilder("=")
                .mathType(BOOLEAN_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull public final PApply formImplies(PExp left, PExp right) {
        PExp functionPortion = new PSymbolBuilder("implies")
                .mathType(BOOLEAN_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull public final PSymbol formConcExp() {
        return new PSymbolBuilder("conc").mathType(BOOLEAN).build();
    }

    @NotNull public final PApply formInitializationPredicate(
            @NotNull PTType argType,
            @NotNull String argName) {
        PSymbol predicateArg = new PSymbolBuilder(argName)
                .mathType(argType.toMath()).build();
        MTFunction initType = new MTFunctionBuilder(this, BOOLEAN)
                .paramTypes(argType.toMath()).build();
        PSymbol namePortion = new PSymbolBuilder(argType + ".Is_Initial")
                .mathType(initType).build();
        return new PApplyBuilder(namePortion).arguments(predicateArg)
                .applicationType(BOOLEAN).build();
    }
}
