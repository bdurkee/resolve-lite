package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.codegen.model.OutputModelObject;
import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.List;

public class AssertiveCode extends OutputModelObject {

    @ModelElement private final List<VCRuleTargetedStat> verificationStats =
            new ArrayList<>();
    @ModelElement private final VCConfirm confirm;
    private final TypeGraph g;
    private final ParserRuleContext ctx;

    public AssertiveCode(TypeGraph g, ParserRuleContext ctx) {
        this.g = g;
        this.ctx = ctx;
        this.confirm = new VCConfirm(g.getTrueExp(), this);
    }

    public TypeGraph getTypeGraph() {
        return g;
    }

    public ParserRuleContext getDefiningCtx() {
        return ctx;
    }

    public VCConfirm getConfirm() {
        return confirm;
    }

}
