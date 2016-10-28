package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.MathClssftn;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class VCClassftnPrintingListener extends PExpListener {

    private final RESOLVECompiler compiler;

    public VCClassftnPrintingListener(@NotNull RESOLVECompiler rc) {
        this.compiler = rc;
    }

    @Override
    public void beginPSymbol(PSymbol e) {
        printClssftn(e);
    }

    @Override
    public void beginPApply(PApply e) {
        printClssftn(e);
    }

    private void printClssftn(@NotNull PExp e) {
        MathClssftn t = e.getMathClssftn();

        String colonOp = " : ";
        if (t == t.getTypeGraph().CLS) {
            colonOp = " ⦂ ";
        }
        compiler.errMgr.info(e.toString() + colonOp + t.toString());
    }
}
