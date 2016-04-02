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
    public final MathClassification INVALID = MathInvalidClassification.getInstance(this);

    public final MathClassification CLS = new MathNamedClassification(this, "Cls", 2, INVALID);
    public final MathClassification SSET = new MathNamedClassification(this, "SSet", 2, CLS);

    public final MathClassification ENTITY = new MathNamedClassification(this, "Entity", 1, INVALID);
    public final MathClassification EL = new MathNamedClassification(this, "El", 1, INVALID);

    public final MathClassification BOOLEAN = new MathNamedClassification(this, "B", 1, SSET);
    public final MathClassification VOID = new MathNamedClassification(this, "Void", 0, INVALID);

    /** General purpose (binary) boolean function type, useful for things like
     *  "and", "or", "xor", etc;
     */
    public final MathFunctionClassification BOOLEAN_FUNCTION =
            new MathFunctionClassification(this, BOOLEAN, BOOLEAN, BOOLEAN);
    public final MathFunctionClassification EQUALITY_FUNCTION =
            new MathFunctionClassification(this, BOOLEAN, ENTITY, ENTITY);
    public final MathFunctionClassification POWERSET_FUNCTION =
            new MathFunctionClassification(this, POWERSET_APPLICATION, SSET, SSET);
    public final MathFunctionClassification ARROW_FUNCTION =
            new MathFunctionClassification(this, ARROW_APPLICATION, CLS, CLS, CLS);
    public final MathFunctionClassification CROSS_PROD_FUNCTION =
            new MathFunctionClassification(this, CARTESIAN_APPLICATION, CLS, CLS, CLS);

    private final static FunctionApplicationFactory CARTESIAN_APPLICATION =
            new CartesianProductApplicationFactory();
    private final static FunctionApplicationFactory POWERSET_APPLICATION =
            new PowersetApplicationFactory();
    private final static FunctionApplicationFactory ARROW_APPLICATION =
            new ArrowApplicationFactory();

    private static class PowersetApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathClassification buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionClassification f,
                @NotNull String calledAsName,
                @NotNull List<MathClassification> arguments) {
            return new MathPowersetApplicationClassification(g, arguments.get(0));
        }
    }

    private static class ArrowApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathClassification buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionClassification f,
                @NotNull String calledAsName,
                @NotNull List<MathClassification> arguments) {
            return new MathFunctionClassification(g, arguments.get(1), arguments.get(0));
        }
    }

    private static class CartesianProductApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathClassification buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionClassification f,
                @NotNull String calledAsName,
                @NotNull List<MathClassification> arguments) {
            return new MathCartesianClassification(g, arguments);
        }
    }

    public boolean isSubtype(@NotNull MathClassification subtype,
                             @NotNull MathClassification supertype) {
        boolean result = (supertype == ENTITY || supertype == CLS);
        if ( !result ) {
            MathClassification subtypesEnclosingType = subtype.enclosingClassification;

            //if we're equal, we're a trivial subtype
            if (subtype.equals(supertype)) result = true;
            else if (subtypesEnclosingType != null &&
                    subtypesEnclosingType.equals(supertype)) {
                result = true;
            }
            else if (subtype instanceof MathFunctionApplicationClassification &&
                    supertype instanceof MathFunctionApplicationClassification) {
                result = isSubtype(subtype.getEnclosingClassification(),
                        supertype.getEnclosingClassification());
            }
            else if (subtype instanceof MathFunctionClassification &&
                    supertype instanceof MathFunctionClassification) {
                result = isSubtype(((MathFunctionClassification) subtype).getDomainType(),
                        ((MathFunctionClassification) supertype).getDomainType())
                    && isSubtype(((MathFunctionClassification) subtype).getResultType(),
                        ((MathFunctionClassification) supertype).getResultType());
            }

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
