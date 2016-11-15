package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.app.RememberApplicationStrategy;
import org.jetbrains.annotations.NotNull;

public class VCRemember extends VCRuleBackedStat {

    public VCRemember(VCAssertiveBlockBuilder block) {
        super(null, block, new RememberApplicationStrategy());
    }

    @NotNull
    public VCRemember copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCRemember(b);
    }

    @Override
    public String toString() {
        return "Remember;";
    }
}
