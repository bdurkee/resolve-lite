package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.misc.Utils;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.applicationstrategies.AssumeApplicationStrategy;

import java.util.ArrayList;
import java.util.List;

public class VCAssertiveBlock extends AbstractAssertiveCode {

    private VCAssertiveBlock(AssertiveBlockBuilder builder) {
        super(builder.getTypeGraph(), builder.getDefiningCtx(),
                builder.getConfirm());
    }

    public static class AssertiveBlockBuilder extends AbstractAssertiveCode
            implements
                Utils.Builder<VCAssertiveBlock> {

        protected final List<VCRuleTargetedStat> verificationStats =
                new ArrayList<>();

        public AssertiveBlockBuilder(TypeGraph g, ParserRuleContext ctx) {
            super(g, ctx);
        }

        public AssertiveBlockBuilder assume(PExp assume) {
            verificationStats.add(new VCAssume(assume, this));
            return this;
        }

        /**
         * Applies the appropriate rule to each
         */
        @Override public VCAssertiveBlock build() {
            for (VCRuleTargetedStat rulestat : verificationStats) {
                rulestat.reduce();
            }
            return new VCAssertiveBlock(this);
        }
    }
}
