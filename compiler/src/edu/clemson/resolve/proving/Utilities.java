package edu.clemson.resolve.proving;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.vcgen.VC;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Utilities {

    public static PExp replacePExp(PExp p, DumbMathClssftnHandler g, MathClssftn z, MathClssftn n) {
        ArrayList<PExp> argList = new ArrayList<>();
        ArrayList<PExp> argsTemp = new ArrayList<>();
        for (PExp pa : p.getSubExpressions()) {
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
        //TODO: Finish these transformations
        return p;
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
