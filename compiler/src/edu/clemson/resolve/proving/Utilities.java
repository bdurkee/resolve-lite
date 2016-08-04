package edu.clemson.resolve.proving;

import edu.clemson.resolve.proving.absyn.*;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.MathFunctionClssftn;
import edu.clemson.resolve.semantics.Quantification;
import edu.clemson.resolve.vcgen.VC;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utilities {

    public static PExp replacePExp(PExp p, DumbMathClssftnHandler g, MathClssftn z, MathClssftn n) {
        ArrayList<PExp> argList = new ArrayList<>();
        ArrayList<PExp> argsTemp = new ArrayList<>();
        List<? extends PExp> subexps = p.getSubExpressions();
        if (p instanceof PApply) subexps = ((PApply) p).getArguments();

        for (PExp pa : subexps) {
            argList.add(replacePExp(pa, g, z, n));
        }
        String pTop = p.getTopLevelOperationName();
        if (pTop.equals("/=") || pTop.equals("≠")) {
            PSymbol boolEqFuncName = new PSymbol.PSymbolBuilder("=B")
                    .mathClssfctn(g.EQUALITY_FUNCTION).build();
            PApply eqExp = new PApply.PApplyBuilder(boolEqFuncName).arguments(argList)
                    .applicationType(g.BOOLEAN).build();
            argList.clear();
            argList.add(eqExp);
            argList.add(g.getFalseExp());
            PApply trEqF = new PApply.PApplyBuilder(boolEqFuncName).arguments(argList)
                    .applicationType(g.BOOLEAN).build();
        }
        else if (pTop.equals("not") || pTop.equals("⌐")) {
            argList.add(g.getFalseExp());
            PSymbol boolEqFuncName = new PSymbol.PSymbolBuilder("=B")
                    .mathClssfctn(g.EQUALITY_FUNCTION).build();
            PApply pEqFalse = new PApply.PApplyBuilder(boolEqFuncName).arguments(argList)
                    .applicationType(g.BOOLEAN).build();
            return pEqFalse;
        }
        else if (pTop.equals(">=") || pTop.equals("≥")) {
            argsTemp.add(argList.get(1));
            argsTemp.add(argList.get(0));
            MathClssftn x = p instanceof PApply ? ((PApply) p).getFunctionPortion().getMathClssftn() :
                    g.EQUALITY_FUNCTION;
            PSymbol leqB = new PSymbol.PSymbolBuilder("<=B").mathClssfctn(x).build();
            return new PApply.PApplyBuilder(leqB).arguments(argsTemp).applicationType(g.BOOLEAN).build();
        }
        else if (pTop.equals("<") && z != null && n != null
                && argList.get(0).getMathClssftn().isSubtypeOf(z)
                && argList.get(1).getMathClssftn().isSubtypeOf(z)) {
            // x < y to x + 1 <= y
           /* argsTemp.add(argList.get(0));
            argsTemp.add(new PSymbol.PSymbolBuilder("1").mathClssfctn(n).build());
            PSymbol plus1 =
                    new PSymbol(argList.get(0).getType(), null, "+"
                            + argList.get(0).getType().toString(), argsTemp);
            argsTemp.clear();
            argsTemp.add(plus1);
            argsTemp.add(argList.get(1));
            return new PSymbol(p.getType(), p.getTypeValue(), "<=B", argsTemp);*/
        }
        // x - y to x + (-y)
       /* else if (z != null && pTop.equals("-") &&
                p instanceof PApply && ((PApply) p).getArguments().size() == 2) {
            //⨩
            PSymbol uMinus = new PSymbol.PSymbolBuilder("⨩" + z)
                    .mathClssfctn(new MathFunctionClssftn(g, z, z))
                    .build();

            //⨩(y)
            PApply negY = new PApply.PApplyBuilder(uMinus)
                    .arguments(((PApply) p).getArguments().get(1))
                    .applicationType(p.getMathClssftn())
                    .build();

            //+
            MathClssftn argType = ((PApply) p).getArguments().get(0).getMathClssftn();
            PSymbol plus = new PSymbol.PSymbolBuilder("+" + argType)
                    .mathClssfctn(new MathFunctionClssftn(g, argType, argType, argType))
                    .build();

            //x + ⨩(y)
            PApply xPlusNegY = new PApply.PApplyBuilder(plus)
                    .arguments(((PApply) p).getArguments().get(0), negY)
                    .applicationType(p.getMathClssftn())
                    .build();
            return xPlusNegY;
        }*/
        // New: 5/8/16. Tag operators with range type if they aren't quantified.
        else if (argList.size() > 0 && p instanceof PApply) {
            if (p.getQuantification().equals(Quantification.NONE)) pTop += p.getMathClssftn().toString();
            PExp appWithNameChanged = p;

            //TODO: its tough to change the "name" of an operator now -- because they are are expressions as well..
            //hence the ugliness of the if below.
            //maybe change the recursive structure of this method to accomodate this better?
            if (((PApply) p).getFunctionPortion() instanceof PSymbol) {
                PSymbol name = new PSymbol.PSymbolBuilder((PSymbol)((PApply) p)
                        .getFunctionPortion()).name(pTop).build();
                appWithNameChanged = new PApply.PApplyBuilder(name).applicationType(p.getMathClssftn())
                        .arguments(argList).build();
            }
            return appWithNameChanged;
        }
        return p;
    }

    protected static PExp flattenPSelectors(@NotNull PExp e) {
        PSelectorFlattener l = new PSelectorFlattener();
        e.accept(l);
        return e.substitute(l.substitutions);
    }

    public static class PSelectorFlattener extends PExpListener {
        public Map<PExp, PExp> substitutions = new HashMap<>();

        @Override
        public void endPSelector(PSelector e) {
            PSymbol s = new PSymbol.PSymbolBuilder(e.getLeft() + "." + e.getRight())
                    .mathClssfctn(e.getMathClssftn())
                    .quantification(e.getQuantification())
                    .incoming(e.isIncoming())
                    .build();
            substitutions.put(e, s);
        }
    }

    /** Builds and returns a typed {@link PSymbol} for the name/function "=B" */
    protected static PSymbol buildEqBName(@NotNull DumbMathClssftnHandler g) {
        return new PSymbol.PSymbolBuilder("=B").mathClssfctn(g.EQUALITY_FUNCTION).build();
    }

    /** Builds and returns a typed {@link PSymbol} for the name/function "orB" */
    protected static PSymbol buildOrBName(@NotNull DumbMathClssftnHandler g) {
        return new PSymbol.PSymbolBuilder("orB").mathClssfctn(g.BOOLEAN_FUNCTION).build();
    }

    /** Builds and returns a typed {@link PSymbol} for the name/function "andB" */
    protected static PSymbol buildAndBName(@NotNull DumbMathClssftnHandler g) {
        return new PSymbol.PSymbolBuilder("andB").mathClssfctn(g.BOOLEAN_FUNCTION).build();
    }
}
