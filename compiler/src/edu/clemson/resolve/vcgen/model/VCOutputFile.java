package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.proving.Consequent;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VC;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class VCOutputFile extends OutputModelObject {

    private int assertiveBatchCt;

    /**
     * All completed {@link AssertiveBlock} objects; where each represents a vc or group of vcs that must be satisfied
     * to verify a parsed program.
     */
    @ModelElement
    public List<AssertiveBlock> chunks = new ArrayList<>();

    /** The final list of immutable vcs. */
    @ModelElement
    public List<VC> finalVcs = new ArrayList<>();

    public VCOutputFile() {
        assertiveBatchCt = 0;
    }

    public List<VC> getFinalVCs() {
        return this.finalVcs;
    }

    public void addAssertiveBlock(AssertiveBlock b) {
        chunks.add(b);
        addVCsInContext(b, assertiveBatchCt);
        assertiveBatchCt++;
    }

    /**
     * Each {@code AssertiveBlock} contains a set of VCs that refer to the same set of free variables.  This method
     * adds each {@code VC} to the final list.
     *
     * @param batch         The set of {@code VC}s in context.
     * @param sectionNumber The batch number so that we can mirror the numbering used by the Verifier.
     *                      (Ideally, we should eventually embed the name of each {@code VC} from the Verifier with
     *                      its name for greater robustness.)
     */
    private void addVCsInContext(final AssertiveBlock batch, final int sectionNumber) {

        VCConfirm batchedConfirm = batch.getFinalConfirm();
        List<PExp> sequentComponents = batchedConfirm.getConfirmExp().split();
        //System.out.println("FINAL CONF: " + batch.getFinalConfirm().getConfirmExp());
        int vcIndex = 1;
        for (PExp vc : sequentComponents) {
            List<? extends PExp> args = vc.getSubExpressions();
            if (!(vc instanceof PApply)) continue;

            PExp antecedentExp = args.get(1);
            PExp consequentExp = args.get(2);

            VC curVC = new VC(sectionNumber + "_" + vcIndex, antecedentExp, consequentExp);
            if (args.get(2).isObviouslyTrue()) continue;
            finalVcs.add(curVC);
            vcIndex++;
        }
    }


}
