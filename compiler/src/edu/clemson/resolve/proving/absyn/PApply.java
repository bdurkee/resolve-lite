package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;

public class PApply extends PExp {

    private PApply(PApplyBuilder builder) {
        super();

    }

    public static class PApplyBuilder implements Utils.Builder<PApply> {

        @Override public PApply build() {
            return new PApply(this);
        }
    }
}
