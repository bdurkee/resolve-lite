package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgVariableSymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

public class VCAssertiveBlock extends AssertiveCode {

    private VCAssertiveBlock(AssertiveBlockBuilder builder) {
        super(builder.getTypeGraph(), builder.getDefiningCtx());
    }

    public static class AssertiveBlockBuilder extends AssertiveCode
            implements
                Utils.Builder<VCAssertiveBlock> {

        protected final AnnotatedTree annotations;
        protected final Set<PExp> freeVars = new LinkedHashSet<>();
        protected final List<VCRuleTargetedStat> verificationStats =
                new ArrayList<>();

        public AssertiveBlockBuilder(TypeGraph g, ParserRuleContext ctx,
                AnnotatedTree annotations) {
            super(g, ctx);
            this.annotations = annotations;
        }

        public AssertiveBlockBuilder assume(PExp assume) {
            verificationStats.add(new VCAssume(assume, this));
            return this;
        }

        public AssertiveBlockBuilder freeVars(List<? extends Symbol> symbols) {
            List<PExp> asExps =
                    symbols.stream().map(s -> new PSymbolBuilder(s.getName())
                            .mathType(s.toMathSymbol().getType())
                            .build()).collect(Collectors.toList());
            freeVars.addAll(asExps);
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
