package edu.clemson.resolve.semantics;

public class MathPowersetApplicationClssftn extends MathFunctionApplicationClssftn {
    private final MathClssftn argument;

    protected MathPowersetApplicationClssftn(DumbMathClssftnHandler g, MathClssftn argument) {
        super(g, g.POWERSET_FUNCTION, "Powerset", argument);
        int normalResultRefDepth = g.POWERSET_FUNCTION.getRangeClssftn().typeRefDepth - 1;
        //powerset is the normal type ref depth of the range (SSET) + whatever
        //the args type ref depth is...
        this.typeRefDepth = normalResultRefDepth + argument.typeRefDepth;
        this.argument = argument;
    }

    public MathClssftn getPowersetArgumentClassification() {
        return argument;
    }
}
