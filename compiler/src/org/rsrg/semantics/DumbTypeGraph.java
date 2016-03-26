package org.rsrg.semantics;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PApply.DisplayStyle;
import edu.clemson.resolve.proving.absyn.PApply.PApplyBuilder;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DumbTypeGraph {

    public DumbTypeGraph() {}
    public final MathType INVALID = MathInvalidType.getInstance(this);

    public final MathType CLS = new MathNamedType(this, "Cls", 2, INVALID);
    public final MathType SSET = new MathNamedType(this, "SSet", 2, CLS);

    public final MathType ENTITY = new MathNamedType(this, "Entity", 1, INVALID);
    public final MathType EL = new MathNamedType(this, "El", 1, INVALID);

    public final MathType BOOLEAN = new MathNamedType(this, "B", 1, SSET);
    public final MathType VOID = new MathNamedType(this, "Void", 0, INVALID);

    /** General purpose (binary) boolean function type, useful for things like
     *  "and", "or", "xor", etc;
     */
    public final MathFunctionType BOOLEAN_FUNCTION =
            new MathFunctionType(this, BOOLEAN, BOOLEAN, BOOLEAN);
    public final MathFunctionType EQUALITY_FUNCTION =
            new MathFunctionType(this, BOOLEAN, ENTITY, ENTITY);
    public final MathFunctionType POWERSET_FUNCTION =
            new MathFunctionType(this, POWERSET_APPLICATION, SSET, SSET);
    public final MathFunctionType ARROW_FUNCTION =
            new MathFunctionType(this, ARROW_APPLICATION, CLS, CLS, CLS);
    public final MathFunctionType CROSS_PROD_FUNCTION =
            new MathFunctionType(this, CARTESIAN_APPLICATION, CLS, CLS, CLS);

    private final static FunctionApplicationFactory CARTESIAN_APPLICATION =
            new CartesianProductApplicationFactory();
    private final static FunctionApplicationFactory POWERSET_APPLICATION =
            new PowersetApplicationFactory();
    private final static FunctionApplicationFactory ARROW_APPLICATION =
            new ArrowApplicationFactory();

    private static class PowersetApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathType buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionType f,
                @NotNull String calledAsName,
                @NotNull List<MathType> arguments) {
            return new MathPowersetApplicationType(g, arguments.get(0));
        }
    }

    private static class ArrowApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathType buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionType f,
                @NotNull String calledAsName,
                @NotNull List<MathType> arguments) {
            return new MathFunctionType(g, arguments.get(1), arguments.get(0));
        }
    }

    private static class CartesianProductApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathType buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionType f,
                @NotNull String calledAsName,
                @NotNull List<MathType> arguments) {
            return new MathCartesianType(g, arguments);
        }
    }

    public boolean isSubtype(@NotNull MathType subtype,
                             @NotNull MathType supertype) {
        boolean result = (supertype == ENTITY || supertype == CLS);
        if ( !result ) {
            MathType subtypesEnclosingType = subtype.enclosingType;
            if (subtypesEnclosingType != null &&
                    subtypesEnclosingType.equals(supertype)) return true;
        }
        return result;
    }

    @Nullable public PExp formConjuncts(PExp... e) {
        return formConjuncts(Arrays.asList(e));
    }

    @Nullable public PExp formConjuncts(List<PExp> e) {
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
}
