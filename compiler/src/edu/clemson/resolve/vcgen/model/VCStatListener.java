package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;

public abstract class VCStatListener {

    public void beginVCRuleBackedStat(@NotNull VCRuleBackedStat p) {
    }

    public void endVCRuleBackedStat(@NotNull VCRuleBackedStat p) {
    }

    public void beginVC(@NotNull VCRuleBackedStat p) {
    }
}
