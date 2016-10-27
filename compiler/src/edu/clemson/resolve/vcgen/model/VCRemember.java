package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.RememberApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VCRemember extends VCRuleBackedStat {

    public VCRemember(VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        super(null, block, new RememberApplicationStrategy());
    }

    @NotNull
    public VCRemember copyWithBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCRemember(b);
    }
}
