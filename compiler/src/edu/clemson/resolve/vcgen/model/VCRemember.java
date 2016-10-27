package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.RememberApplicationStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VCRemember extends VCRuleBackedStat {

    public VCRemember(VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        super(null, block, new RememberApplicationStrategy());
    }

    public List<PExp> getRememberVars() {
        return statComponents;
    }

    @NotNull
    public VCRemember copyWithBlock(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder b) {
        return new VCRemember(b);
    }
}
