package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.codegen.model.Stat;
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
        super(builder., builder.getDefiningCtx(),
                builder.verificationStats, builder.getConfirm());
    }

    public static class AssertiveBlockBuilder
            implements
                Utils.Builder<VCAssertiveBlock> {

        public final TypeGraph g;
        public final ParserRuleContext definingTree
        public final AnnotatedTree annotations;
        public final Set<PExp> freeVars = new LinkedHashSet<>();
        public final List<VCRuleTargetedStat> verificationStats =
                new ArrayList<>();
        public PExp finalConfirm;

        public AssertiveBlockBuilder(TypeGraph g, ParserRuleContext ctx,
                AnnotatedTree annotations) {
            this.g = g;
            this.definingTree = ctx;
            this.annotations = annotations;
        }

        public AssertiveBlockBuilder assume(PExp assume) {
            verificationStats.add(new VCAssume(assume, this));
            return this;
        }

        public AssertiveBlockBuilder remember() {
            return this;
        }

        public AssertiveBlockBuilder confirm(PExp confirm) {
            verificationStats.add(new VCConfirm(confirm, this));
            return this;
        }

        public AssertiveBlockBuilder finalConfirm(PExp confirm) {
            if (finalConfirm != null) {
                throw new IllegalArgumentException("final confirm already set");
            }
            if (confirm == null) {
                throw new IllegalArgumentException(
                        "final confirm cannot be null");
            }
            this.finalConfirm = confirm;
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

        public AssertiveBlockBuilder stats(List<VCRuleTargetedStat> stats) {
            verificationStats.addAll(stats);
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
