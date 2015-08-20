package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public class VCAssertiveBlock extends AssertiveBlock {

    private VCAssertiveBlock(VCAssertiveBlockBuilder builder) {
        super(builder.g, builder.definingTree, builder.finalConfirm,
                builder.annotations, builder.stats, builder.freeVars,
                builder.applicationSteps, builder.description);
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
        public final String description;

        public VCAssertiveBlockBuilder(TypeGraph g, Scope contextScope,
                                       String description, ParserRuleContext ctx,
                                       AnnotatedTree annotations) {
            this.g = g;
            this.definingTree = ctx;
            this.annotations = annotations;
            this.finalConfirm = new VCConfirm(this, g.getTrueExp());
            this.scope = contextScope;
            this.description = description;
        }

        public VCAssertiveBlockBuilder assume(List<PExp> assumes) {
            assumes.forEach(this::assume);
            return this;
        }

        public VCAssertiveBlockBuilder assume(PExp assume) {
            if ( assume == null ) {
                return this;
            }
            stats.add(new VCAssume(this, assume));
            return this;
        }

        public VCAssertiveBlockBuilder remember() {
            stats.add(new VCRemember(this));
            return this;
        }

        public VCAssertiveBlockBuilder confirm(List<PExp> confirms) {
            confirms.forEach(this::confirm);
            return this;
        }

        public VCAssertiveBlockBuilder confirm(PExp confirm) {
            if ( confirm == null ) {
                confirm = g.getTrueExp();
            }
            stats.add(new VCConfirm(this, confirm));
            return this;
        }

        public VCAssertiveBlockBuilder finalConfirm(PExp confirm) {
            if ( confirm == null ) {
                throw new IllegalArgumentException("finalconfirm==null");
            }
            this.finalConfirm = new VCConfirm(this, confirm);
            return this;
        }

        public VCAssertiveBlockBuilder freeVars(List<? extends Symbol> symbols) {
            List<PSymbol> asPSyms =
                    symbols.stream().map(s -> new PSymbol.PSymbolBuilder(s.getName())
                            .mathType(s.toMathSymbol().getType())
                            .build()).collect(Collectors.toList());
            freeVars.addAll(asPSyms);
            return this;
        }

        public VCAssertiveBlockBuilder stats(List<VCRuleBackedStat> e) {
            for (VCRuleBackedStat stat : e) {
                if ( stat == null ) {
                    throw new IllegalArgumentException("null rule app stat");
                }
                stats.add(stat);
            }
            return this;
        }

        public VCAssertiveBlockBuilder stats(VCRuleBackedStat... e) {
            stats(Arrays.asList(e));
            return this;
        }

        /**
         * Same as {@link #build()}, but this one doesn't automatically apply
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