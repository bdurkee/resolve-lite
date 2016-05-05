package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PLambda;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A visitor that replaces lambda bound variables with their corresponding
 * supplied actuals.
 * <p>
 * Note: This listener will therefore only mutate parts of an expression where
 * the first class name portion of some {@link PApply} is an instance of
 * {@link PLambda}.</p>
 */
public class BasicBetaReducingListener extends PExpListener {

    private PExp reducedExp;

    public BasicBetaReducingListener(@NotNull PExp start) {
        this.reducedExp = start;
    }

    @NotNull
    public PExp getReducedExp() {
        return reducedExp;
    }

    @Override
    public void endPApply(@NotNull PApply e) {
        PExp name = e.getFunctionPortion();
        if (name instanceof PLambda) {
            PLambda asPLambda = (PLambda) name;
            List<PExp> boundVars = asPLambda.getParameters().stream().map(PLambda.MathSymbolDeclaration::asPSymbol)
                    .collect(Collectors.toList());
            reducedExp = reducedExp.substitute(e, asPLambda.getBody().substitute(boundVars, e.getArguments()));
        }
    }
}
