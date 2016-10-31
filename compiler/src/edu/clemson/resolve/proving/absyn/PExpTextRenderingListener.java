package edu.clemson.resolve.proving.absyn;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders expressions in a nice way, with minimal parentheses. You shouldn't have to instantiate and call this class directly,
 * use {@link PExp#render()} instead.
 */
//TODO
public class PExpTextRenderingListener extends PExpListener {

    private final Appendable output;
    private final Map<PExp, String> nodes = new HashMap<>();

    public PExpTextRenderingListener(@NotNull Appendable w) {
        this.output = w;
    }

    @Override
    public void beginPExp(@NotNull PExp e) {

    }

    @Override
    public void endInfixPApply(@NotNull PApply e) {
        String left = nodes.get(e.getArguments().get(0));
        String right = nodes.get(e.getArguments().get(1));
        String name = nodes.get(e.getFunctionPortion());
        //String result = left +
    }

}
