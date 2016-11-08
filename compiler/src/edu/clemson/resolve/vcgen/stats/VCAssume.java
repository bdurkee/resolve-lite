package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.ParsimoniousAssumeApplicationStrategy;
import edu.clemson.resolve.vcgen.app.VCStatRuleApplicationStrategy;
import org.jetbrains.annotations.NotNull;

public class VCAssume extends VCRuleBackedStat {

    private final VCStatRuleApplicationStrategy<VCAssume> apply;
    private final PExp assume;

    private final boolean isStipulatedAssumption, isNotice;

    public VCAssume(VCAssertiveBlockBuilder block, boolean stipulate, PExp assume) {
        this(block, new ParsimoniousAssumeApplicationStrategy(), false, false, assume);
    }

    public VCAssume(VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy<VCAssume> strategy,
                    boolean stipulate,
                    boolean notice,
                    PExp assume) {
        super(null, block, strategy);
        this.apply = strategy;
        this.assume = assume;
        this.isNotice = notice;
        this.isStipulatedAssumption = stipulate;
    }

    public PExp getAssumeExp() {
        return assume;
    }

    public boolean isStipulatedAssumption() {
        return isStipulatedAssumption;
    }

    @NotNull
    public VCAssume copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCAssume(b, apply, isStipulatedAssumption, isNotice, getAssumeExp());
    }

    @Override
    public String toString() {
        String exp = assume.toString(false);
        if (isNotice) {
            return "Notice " + exp + ";";
        }
        String result = "Assume " + exp + ";";
        if (isStipulatedAssumption) {
            result += "Stipulated_" + result;
        }
        return result;
    }
}