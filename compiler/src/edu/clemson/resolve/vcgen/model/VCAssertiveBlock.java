package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.ModelBuilderProto;
import edu.clemson.resolve.vcgen.application.ParsimoniousAssumeApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.Scope;

import java.util.*;

public class VCAssertiveBlock extends AssertiveBlock {

    private VCAssertiveBlock(VCAssertiveBlockBuilder builder) {
        super(builder.definingTree, builder.finalConfirm, builder.applicationSteps, builder.description);
    }

    public static class VCAssertiveBlockBuilder implements Utils.Builder<VCAssertiveBlock> {

        public final DumbMathClssftnHandler g;
        public final ParserRuleContext definingTree;
        public final Scope scope;
        public VCConfirm finalConfirm;
        public List<VCIfElse> ifElses = new ArrayList<>();

        final Map<String, Map<PExp, PExp>> facilitySpecializations = new HashMap<>();
        final LinkedList<VCRuleBackedStat> stats = new LinkedList<>();
        final List<RuleApplicationStep> applicationSteps = new ArrayList<>();
        final String description;

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
            this.finalConfirm = o.finalConfirm;
            this.scope = o.scope;
            this.description = o.description;

            //the stats need to be deep copied with 'this' as the enclosingblock
            for (VCRuleBackedStat s : o.stats) {
                if (s instanceof VCAssume) this.assume(s.statComponents);
                else if (s instanceof VCRemember) this.remember();
                else if (s instanceof VCConfirm) this.confirm(s.getDefiningContext(), ((VCConfirm) s).getConfirmExp());
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
         * Applies the appropriate rule to each stat within this builder. In other words, a call to this will fully
         * develop the final confirm for this particular block of assertive code.
         */
        @NotNull
        @Override
        public VCAssertiveBlock build() {
            applicationSteps.add(new RuleApplicationStep(this.snapshot(), ""));
            while (!stats.isEmpty()) {
                VCRuleBackedStat currentStat = stats.removeLast();
                applicationSteps.add(new RuleApplicationStep(currentStat.applyBackingRule(),
                        currentStat.getApplicationDescription()));
            }
            return new VCAssertiveBlock(this);
        }

            //TODO : Trying to get duplicate blocks for branches while processing the statements in reverse..
        //this seems like one of the better ways to handle the branching if stmts.
        @NotNull
        public List<VCAssertiveBlock> buildWithBranches() {
            List<VCAssertiveBlock> result = new ArrayList<>();
            ListIterator<VCRuleBackedStat> iter = stats.listIterator(stats.size());

            while (iter.hasPrevious()) {

                VCRuleBackedStat currentStat = iter.previous();
                if (currentStat instanceof VCIfElse) {
                }
                if (currentStat instanceof VCIfElse) {
                    stats.addLast(new VCIfElse((VCIfElse) currentStat, ModelBuilderProto.ELSE_APPLICATION));
                }
                applicationSteps.add(new RuleApplicationStep(currentStat.applyBackingRule(),
                        currentStat.getApplicationDescription()));
            }
            return new VCAssertiveBlock(this);
        }

    }
}