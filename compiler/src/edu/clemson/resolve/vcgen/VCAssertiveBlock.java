package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.ParsimoniousAssumeApplicationStrategy;
import edu.clemson.resolve.vcgen.stats.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.Scope;

import java.util.*;

public class VCAssertiveBlock extends AssertiveBlock {

    private VCAssertiveBlock(VCAssertiveBlockBuilder builder) {
        super(builder.definingTree, builder.finalConfirm, builder.applicationSteps,
                builder.stats, builder.description);
    }

    public static class VCAssertiveBlockBuilder implements Utils.Builder<List<VCAssertiveBlock>> {

        public final DumbMathClssftnHandler g;
        public final ParserRuleContext definingTree;
        public final Scope scope;
        public VCConfirm finalConfirm;

        public final Map<String, Map<PExp, PExp>> facilitySpecializations = new HashMap<>();
        protected final LinkedList<VCRuleBackedStat> stats = new LinkedList<>();
        protected final List<RuleApplicationStep> applicationSteps = new ArrayList<>();
        protected final String description;

        public final Deque<VCAssertiveBlockBuilder> branchingBlocks = new LinkedList<>();

        public Map<PExp, PExp> getSpecializationsForFacility(String facility) {
            Map<PExp, PExp> result = facilitySpecializations.get(facility);
            if (result == null) result = new HashMap<>();
            return result;
        }

        public VCAssertiveBlockBuilder(DumbMathClssftnHandler g, Scope s, String description, ParserRuleContext ctx) {
            if (s == null) {
                throw new IllegalArgumentException("passed null scope to vc assertive block for: " + description);
            }
            this.g = g;
            this.definingTree = ctx;
            this.finalConfirm = new VCConfirm(ctx, this, g.getTrueExp());
            this.scope = s;
            this.description = description;
        }

        /**
         * A copy constructor for an assertive block builder.
         *
         * @param o The {@code VCAssertiveBlockBuilder} from which {@code this} will be initialized.
         */
        public VCAssertiveBlockBuilder(VCAssertiveBlockBuilder o) {
            this.g = o.g;
            this.definingTree = o.definingTree;
            this.finalConfirm = o.finalConfirm.copyWithEnclosingBlock(this);
            this.scope = o.scope;
            this.description = o.description;

            //deep copy stats with this block as enclosing..
            for (VCRuleBackedStat s : o.stats) {
                this.stats.add(s.copyWithEnclosingBlock(this));
            }
            this.applicationSteps.addAll(o.applicationSteps);
            this.facilitySpecializations.putAll(o.facilitySpecializations);
        }

        public VCAssertiveBlockBuilder facilitySpecializations(Map<String, Map<PExp, PExp>> mappings) {
            facilitySpecializations.putAll(mappings);
            return this;
        }

        public VCAssertiveBlockBuilder assume(Collection<PExp> assumes) {
            assumes.forEach(this::assume);
            return this;
        }

        public VCAssertiveBlockBuilder assume(PExp assume) {
            if (assume == null) {
                return this;
            }
            //stats.add(new VCAssume(this, new DefaultAssumeApplicationStrategy(), assume));
            stats.add(new VCAssume(this, new ParsimoniousAssumeApplicationStrategy(), assume));
            return this;
        }

        public VCAssertiveBlockBuilder remember() {
            stats.add(new VCRemember(this));
            return this;
        }

        public VCAssertiveBlockBuilder confirm(ParserRuleContext ctx, Collection<PExp> confirms) {
            Utils.apply(confirms, e -> confirm(ctx, e));
            return this;
        }

        public VCAssertiveBlockBuilder confirm(ParserRuleContext ctx, PExp confirm) {
            if (confirm == null) {
                confirm = g.getTrueExp();
            }
            stats.add(new VCConfirm(ctx, this, confirm));
            return this;
        }

        public VCAssertiveBlockBuilder finalConfirm(PExp confirm) {
            if (confirm == null) {
                throw new IllegalArgumentException("finalconfirm==null");
            }
            this.finalConfirm = new VCConfirm(definingTree, this, confirm);
            return this;
        }

        public VCAssertiveBlockBuilder stats(List<VCRuleBackedStat> e) {
            for (VCRuleBackedStat stat : e) {
                if (stat == null) {
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
         * Same as {@link #build()}, but this one doesn't automatically apply proof rules to the stats within this
         * block.
         */
        public VCAssertiveBlock snapshot() {
            return new VCAssertiveBlock(this);
        }

        /**
         * Applies the appropriate rule to each stat within this builder
         * (and also the rules for any branches arising from this). In other words, a call to this will fully
         * develop the final confirm for this particular block of assertive code, as well as its branching blocks.
         */
        @NotNull
        @Override
        public List<VCAssertiveBlock> build() {
            List<VCAssertiveBlock> result = loopAssertiveStack();
            return result;
        }

        //TODO: We need another apply method for rules which takes a stack to track branches that is distinct from
        //assertive code!
        private List<VCAssertiveBlock> loopAssertiveStack() {
            List<VCAssertiveBlock> result = new ArrayList<>();
            Deque<VCAssertiveBlockBuilder> branches = new LinkedList<>();

            branches.push(this);
            while (!branches.isEmpty()) {
                VCAssertiveBlockBuilder curr = branches.pop();
                result.add(curr.applyRules(branches));
                int i;
                i=0;
            }
            return result;
        }

        private VCAssertiveBlock applyRules(Deque<VCAssertiveBlockBuilder> branchAccumulator) {
            //in case we're applying rules in a block arising from a branch, I want to
            //get rid of excess (prior) applications..
            applicationSteps.add(new RuleApplicationStep(this.snapshot().toString(), "Start"));
            while (!stats.isEmpty()) {
                VCRuleBackedStat currentStat = stats.removeLast();
                applicationSteps.add(new RuleApplicationStep(
                        currentStat.applyBackingRule(branchAccumulator).toString(),
                        currentStat.getApplicationDescription()));
            }
            return new VCAssertiveBlock(this);
        }


    }
}