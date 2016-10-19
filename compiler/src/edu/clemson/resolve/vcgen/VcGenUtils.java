package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by daniel on 10/19/16.
 */
public class VcGenUtils {

    //TODO: TEST THIS
    public static boolean inSimpleForm(@NotNull PExp ensures, @NotNull List<ProgParameterSymbol> params) {
        boolean simple = false;
       /* if (ensures instanceof PApply) {
            PApply ensuresAsPApply = (PApply) ensures;
            List<PExp> args = ensuresAsPApply.getArguments();
            if (ensuresAsPApply.isEquality()) {
                if (inSimpleForm(args.get(0), params)) simple = true;
            }
            else if (ensuresAsPApply.isConjunct()) {
                if (inSimpleForm(args.get(0), params) && inSimpleForm(args.get(1), params)) simple = true;
            }
        }
        else if (ensures instanceof PSymbol) {
            for (ProgParameterSymbol p : params) {
                if (p.getMode() == ParameterMode.UPDATES && p.asPSymbol().equals(ensures)) simple = true;
            }
        }*/
        return simple;
    }
}
