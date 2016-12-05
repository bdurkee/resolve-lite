package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.*;
import edu.clemson.resolve.proving.absyn.PLambda.MathSymbolDeclaration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NestedLambdaCollapsingListener extends PExpListener {

    List<PExp> conditions = new ArrayList<>();

    private PExp reducedExp;

    public NestedLambdaCollapsingListener(@NotNull PExp start) {
        this.reducedExp = start;
    }

    @NotNull
    public PExp getReducedExp() {
        return reducedExp;
    }

    @Override
    public void endPLambda(@NotNull PLambda e) {
        //conditions, result
        if (e.getBody() instanceof PAlternatives) {
            //conditions.add(((PAlternatives)e.getBody().getC)
        }
    }
}
