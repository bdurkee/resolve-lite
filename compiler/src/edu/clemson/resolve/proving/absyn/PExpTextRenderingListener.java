package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.codegen.AbstractCodeGenerator;
import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders expressions in a nice way, with minimal parentheses and (reasonably sensible) newlines.
 * You shouldn't have to instantiate and call this class directly, use {@link PExp#render()} instead.
 */
public class PExpTextRenderingListener extends PExpListener {

    private final STGroup g = new STGroupFile(AbstractCodeGenerator.TEMPLATE_ROOT + "/PExp.stg");
    private final Map<PExp, ST> nodes = new HashMap<>();

    public PExpTextRenderingListener(int lineWidth) {
    }

    @Override
    public void endInfixPApply(@NotNull PApply e) {
        ST s = g.getInstanceOf("InfixPApply");
        s.add("left", nodes.get(e.getArguments().get(1)));
        s.add("right", nodes.get(e.getArguments().get(2)));
        nodes.put(e, s);
    }

    @Override
    public void endPrefixPApply(@NotNull PApply e) {
        ST s = g.getInstanceOf("PrefixPApply");
        s.add("name", nodes.get(e.getFunctionPortion()));
        s.add("args", Utils.apply(e.getArguments(), nodes::get));
        nodes.put(e, s);
    }

    @Override
    public void endOutfixPApply(@NotNull PApply e) {
        ST s = g.getInstanceOf("OutfixPApply");
        PSymbol name = (PSymbol) e.getFunctionPortion();
        s.add("left", name.getLeftPrint());
        s.add("right", name.getRightPrint());
        s.add("arg", nodes.get(e.getArguments().get(0)));
        nodes.put(e, s);
    }

    @Override
    public void endPSelector(@NotNull PSelector e) {
        ST s = g.getInstanceOf("PSelector");
        s.add("left", nodes.get(e.getLeft()));
        s.add("right", nodes.get(e.getRight()));
        nodes.put(e, s);
    }

    @Override
    public void endPLambda(@NotNull PLambda e) {
        ST s = g.getInstanceOf("PLambda");
        PLambda.MathSymbolDeclaration parameter = e.getParameters().get(0);
        s.add("var", parameter.getName());
        s.add("right", parameter.getClssftn().toString());
        s.add("body", e.getBody());
        nodes.put(e, s);
    }

    @Override
    public void endPAlternatives(@NotNull PAlternatives e) {
        /*ST s = g.getInstanceOf("PLambda");
        PLambda.MathSymbolDeclaration parameter = e.getParameters().get(0);
        s.add("var", parameter.getName());
        s.add("right", parameter.getClssftn().toString());
        s.add("body", e.getBody());
        nodes.put(e, s);*/
    }

    @NotNull
    private String getTemplateFor(@NotNull PExp e) {
        return e.getClass().getSimpleName();
    }
}
