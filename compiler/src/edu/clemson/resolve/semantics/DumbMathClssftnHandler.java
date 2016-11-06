package edu.clemson.resolve.semantics;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PApply.DisplayStyle;
import edu.clemson.resolve.proving.absyn.PApply.PApplyBuilder;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DumbMathClssftnHandler {

    public DumbMathClssftnHandler() {
    }

    public final MathClssftn INVALID = MathInvalidClssftn.getInstance(this);

    public final MathClssftn CLS = new MathNamedClssftn(this, "Cls", 2, INVALID);
    public final MathClssftn SSET = new MathNamedClssftn(this, "SSet", 2, CLS);
    public final MathClssftn EMPTY_SET = new MathNamedClssftn(this, "Empty_Set", 1, SSET);

    public final MathClssftn ENTITY = new MathNamedClssftn(this, "Entity", 1, INVALID);
    public final MathClssftn EL = new MathNamedClssftn(this, "El", 1, CLS);

    public final MathClssftn BOOLEAN = new MathNamedClssftn(this, "B", 1, SSET);
    public final MathClssftn VOID = new MathNamedClssftn(this, "Void", 0, INVALID);

    /**
     * General purpose (binary) boolean function type, useful for things like
     * "and", "or", "xor", etc;
     */
    public final MathFunctionClssftn BOOLEAN_FUNCTION =
            new MathFunctionClssftn(this, BOOLEAN, BOOLEAN, BOOLEAN);
    public final MathFunctionClssftn EQUALITY_FUNCTION =
            new MathFunctionClssftn(this, BOOLEAN, ENTITY, ENTITY);
    public final MathFunctionClssftn POWERSET_FUNCTION =
            new MathFunctionClssftn(this, POWERSET_APPLICATION, SSET, SSET);
    public final MathFunctionClssftn ARROW_FUNCTION =
            new MathFunctionClssftn(this, ARROW_APPLICATION, CLS, CLS, CLS);
    public final MathFunctionClssftn CROSS_PROD_FUNCTION =
            new MathFunctionClssftn(this, CARTESIAN_APPLICATION, CLS, CLS, CLS);

    private final static FunctionApplicationFactory CARTESIAN_APPLICATION = new CartesianProductApplicationFactory();
    private final static FunctionApplicationFactory POWERSET_APPLICATION = new PowersetApplicationFactory();
    private final static FunctionApplicationFactory ARROW_APPLICATION = new ArrowApplicationFactory();

    private static class PowersetApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override
        public MathClssftn buildFunctionApplication(@NotNull DumbMathClssftnHandler g,
                                                    @NotNull MathFunctionClssftn f,
                                                    @NotNull String calledAsName,
                                                    @NotNull List<MathClssftn> arguments) {
            return new MathPowersetApplicationClssftn(g, arguments.get(0));
        }
    }

    private static class ArrowApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override
        public MathClssftn buildFunctionApplication(@NotNull DumbMathClssftnHandler g,
                                                    @NotNull MathFunctionClssftn f,
                                                    @NotNull String calledAsName,
                                                    @NotNull List<MathClssftn> arguments) {
            return new MathFunctionClssftn(g, arguments.get(1), arguments.get(0));
        }
    }

    private static class CartesianProductApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override
        public MathClssftn buildFunctionApplication(@NotNull DumbMathClssftnHandler g,
                                                    @NotNull MathFunctionClssftn f,
                                                    @NotNull String calledAsName,
                                                    @NotNull List<MathClssftn> arguments) {
            return new MathCartesianClssftn(g,
                    new MathCartesianClssftn.Element(arguments.get(0)),
                    new MathCartesianClssftn.Element(arguments.get(1)));
        }
    }

    public final Map<MathClssftn, List<MathClssftn>> relationships = new HashMap<>();
    public void addRelationship(MathClssftn s, MathClssftn t) {
        if (relationships.get(s) == null) {
            List<MathClssftn> l = new ArrayList<>();
            l.add(t);
            relationships.put(s, l);
        }
        else {
            relationships.get(s).add(t);
        }
    }
    public boolean isSubtype(@NotNull MathClssftn subtype, @NotNull MathClssftn supertype) {
        boolean result = (supertype == ENTITY || supertype == CLS || supertype == EL);
        if (!result) {
            MathClssftn subtypesEnclosingType = subtype.enclosingClassification;

            //hook this in to handle a more complex subtype assertion of the form:
            // Math Classification Assertion: Forall T : SSet, F(T) : X
            //where X is a free variable and T is bound
            for (MathClssftn e : relationships.keySet()) {
                if (e instanceof MathFunctionApplicationClssftn &&
                        subtype instanceof MathFunctionApplicationClssftn) {
                    MathFunctionApplicationClssftn eAsFunctionApp = (MathFunctionApplicationClssftn)e;
                    MathFunctionApplicationClssftn subtypeAsFunctionApp = (MathFunctionApplicationClssftn)subtype;
                    if (eAsFunctionApp.getFunction().equals(subtypeAsFunctionApp.getFunction()) &&
                            eAsFunctionApp.getName().equals(subtypeAsFunctionApp.getName())) {
                        return true;
                    }
                }
            }

            //first check relationships
            if (relationships.get(subtype) != null) {
                for (MathClssftn r : relationships.get(subtype)) {
                    if (r.equals(supertype)) {
                        return true;
                    }
                }
            }

            if (subtype.equals(supertype)) result = true;

            //not too sure about the two below..
            //1
            else if (supertype == SSET && subtype.enclosingClassification == SSET) {
                result = true;
            }
            //2
            else if (subtype.enclosingClassification == supertype) {
                result = true;
            }
            else if (subtype instanceof MathFunctionApplicationClssftn &&
                    supertype instanceof MathFunctionApplicationClssftn) {
                result = isSubtype(subtype.getEnclosingClassification(),
                        supertype.getEnclosingClassification());
            }
            else if (subtype instanceof MathFunctionClssftn &&
                    supertype instanceof MathFunctionClssftn) {
                result = isSubtype(((MathFunctionClssftn) subtype).getDomainType(),
                        ((MathFunctionClssftn) supertype).getDomainType())
                        && isSubtype(((MathFunctionClssftn) subtype).getRangeClssftn(),
                        ((MathFunctionClssftn) supertype).getRangeClssftn());
            }
        }
        return result;
    }

    @Nullable
    public PExp formConjuncts(PExp... e) {
        return formConjuncts(Arrays.asList(e));
    }

    @Nullable
    public PExp formConjuncts(List<PExp> e) {
        if (e == null) {
            throw new IllegalArgumentException("can't conjunct a null list");
        }
        if (e.isEmpty()) return null;
        Iterator<PExp> segsIter = e.iterator();
        PExp result = segsIter.next();
        if (e.size() == 1) {
            return e.get(0);
        }
        while (segsIter.hasNext()) {
            result = formConjunct(result, segsIter.next());
        }
        return result;
    }

    @NotNull
    public PApply formConjunct(@NotNull PExp left, @NotNull PExp right) {
        return new PApplyBuilder(new PSymbolBuilder("∧").mathClssfctn(BOOLEAN_FUNCTION).build())
                .applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull
    public PApply formDisjunct(PExp left, PExp right) {
        return new PApplyBuilder(new PSymbolBuilder("∨").mathClssfctn(BOOLEAN_FUNCTION).build())
                .applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull
    public final PSymbol getTrueExp() {
        return new PSymbolBuilder("true").mathClssfctn(BOOLEAN).literal(true).build();
    }

    @NotNull
    public final PSymbol getFalseExp() {
        return new PSymbolBuilder("false").mathClssfctn(BOOLEAN).literal(true).build();
    }

    @NotNull
    public final PApply formEquals(PExp left, PExp right) {
        return new PApplyBuilder(new PSymbolBuilder("=").mathClssfctn(BOOLEAN_FUNCTION).build())
                .applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull
    public final PApply formImplies(PExp left, PExp right) {
        return new PApplyBuilder(new PSymbolBuilder("⟹").mathClssfctn(BOOLEAN_FUNCTION).build())
                .applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull
    public final PSymbol formConcExp() {
        return new PSymbolBuilder("conc").mathClssfctn(BOOLEAN).build();
    }
}
