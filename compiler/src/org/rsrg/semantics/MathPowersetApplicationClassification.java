package org.rsrg.semantics;

public class MathPowersetApplicationClassification extends MathFunctionApplicationClassification {
    private final MathClassification argument;

    protected MathPowersetApplicationClassification(DumbTypeGraph g, MathClassification argument) {
        super(g, g.POWERSET_FUNCTION, "Powerset", argument);
        int normalResultRefDepth =
                g.POWERSET_FUNCTION.getResultType().typeRefDepth - 1;
        //powerset is the normal type ref depth of the range (SSET) + whatever
        //the args type ref depth is...
        this.typeRefDepth = normalResultRefDepth + argument.typeRefDepth;
        this.argument = argument;
    }

    public MathClassification getPowersetArgumentClassification() {
        return argument;
    }
}
