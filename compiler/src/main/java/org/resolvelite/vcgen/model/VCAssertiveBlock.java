package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.applicationstrategies.RememberApplicationStrategy;

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
        public final Scope scope;
        public final AnnotatedTree annotations;
        public VCConfirm finalConfirm;

        public final Set<PSymbol> freeVars = new LinkedHashSet<>();
        public final LinkedList<VCRuleBackedStat> stats = new LinkedList<>();
        public final List<RuleApplicationStep> applicationSteps =
                new ArrayList<>();

        public VCAssertiveBlockBuilder(TypeGraph g, Scope contextScope,
                                       ParserRuleContext ctx,
                AnnotatedTree annotations) {
            this.g = g;
            this.definingTree = ctx;
            this.annotations = annotations;
            this.finalConfirm = new VCConfirm(g.getTrueExp(), this);
            this.scope = contextScope;
        }

        public VCAssertiveBlockBuilder assume(PExp assume) {
            stats.add(new VCAssume(assume, this));
            return this;
        }

        public VCAssertiveBlockBuilder remember() {
            VCRemember remember = new VCRemember(null, this);
            //Todo: not too sure if it's important where remember falls in
            //the stat sequence..
            if ( stats.size() > 1 ) {
                stats.add(1, remember);
            }
            else {
                stats.add(remember);
            }
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
            List<PSymbol> asPSyms =
                    symbols.stream().map(s -> new PSymbolBuilder(s.getName())
                            .mathType(s.toMathSymbol().getType())
                            .build()).collect(Collectors.toList());
            freeVars.addAll(asPSyms);
            return this;
        }

        public VCAssertiveBlockBuilder stats(List<VCRuleBackedStat> e) {
            this.stats.addAll(e);
            return this;
        }

        /**
         * Same as {@link #build()} but this one doesn't automatically apply
         * proof rules to the stats within this block.
         */
        public VCAssertiveBlock snapshot() {
            return new VCAssertiveBlock(this);
        }

        /**
         * Applies the appropriate rule to each stat within this builder. In
         * other words, a call to this will fully develop the final confirm
         * for this particular block of assertive code.
         */
        @Override public VCAssertiveBlock build() {
            applicationSteps.add(new RuleApplicationStep(this.snapshot(), ""));
            while (!stats.isEmpty()) {
                VCRuleBackedStat currentStat = stats.removeLast();
                applicationSteps.add(new RuleApplicationStep(currentStat
                        .reduce(), currentStat.getApplicationDescription()));
            }
            return new VCAssertiveBlock(this);
        }
    }
}
