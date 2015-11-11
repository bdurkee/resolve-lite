package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VCTranslator extends PExpListener {

    public VCTranslator(VC vc) {

    }

    @Override public void beginPApply(@NotNull PApply e) {

    }
}
