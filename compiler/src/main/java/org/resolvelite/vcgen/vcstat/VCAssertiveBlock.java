package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

public class VCAssertiveBlock extends AssertiveCode {

    private VCAssertiveBlock(VCAssertiveBlockBuilder builder) {
        super(builder.g, builder.definingTree, builder.finalConfirm,
                builder.annotations, builder.stats, builder.freeVars,
                builder.applicationSteps);
    }

    public static class VCAssertiveBlockBuilder
            implements
                Utils.Builder<VCAssertiveBlock> {

        public final TypeGraph g;
        public final ParserRuleContext definingTree;
        public final AnnotatedTree annotations;
        public final Set<PExp> freeVars = new LinkedHashSet<>();
        public final LinkedList<VCRuleBackedStat> stats = new LinkedList<>();
        public VCConfirm finalConfirm;

        public List<AssertiveCode> applicationSteps = new ArrayList<>();

        public VCAssertiveBlockBuilder(TypeGraph g, ParserRuleContext ctx,
                AnnotatedTree annotations) {
            this.g = g;
            this.definingTree = ctx;
            this.annotations = annotations;
        }

        public VCAssertiveBlockBuilder assume(PExp assume) {
            stats.add(new VCAssume(assume, this));
            return this;
        }

        public VCAssertiveBlockBuilder remember() {
            return this;
        }

        public VCAssertiveBlockBuilder confirm(PExp confirm) {
            stats.add(new VCConfirm(confirm, this));
            return this;
        }

        public VCAssertiveBlockBuilder finalConfirm(PExp confirm) {
            if ( confirm == null ) {
                throw new IllegalArgumentException(
                        "final confirm cannot be null");
            }
            this.finalConfirm = new VCConfirm(confirm, this);
            return this;
        }

        public VCAssertiveBlockBuilder freeVars(List<? extends Symbol> symbols) {
            List<PExp> asExps =
                    symbols.stream().map(s -> new PSymbolBuilder(s.getName())
                            .mathType(s.toMathSymbol().getType())
                            .build()).collect(Collectors.toList());
            freeVars.addAll(asExps);
            return this;
        }

        public VCAssertiveBlockBuilder stats(List<VCRuleBackedStat> e) {
            this.stats.addAll(e);
            return this;
        }

        public VCAssertiveBlock snapshot() {
            return new VCAssertiveBlock(this);
        }

        /**
         * Applies the appropriate rule to each
         */
        @Override public VCAssertiveBlock build() {
            applicationSteps.add(this.snapshot());
            while (!stats.isEmpty()) {
                VCRuleBackedStat currentStat = stats.removeLast();
                applicationSteps.add(currentStat.reduce());
            }
            return new VCAssertiveBlock(this);
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("free vars: ");
            for (PExp var : freeVars) {
                sb.append(var + " : " + var.getMathType()).append(", ");
            }
            sb.append("\n");
            for (VCRuleBackedStat s : stats) {
                sb.append(s).append("\n");
            }
            return sb.append(finalConfirm).toString();
        }
    }
}
