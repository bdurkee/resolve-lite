package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VC;

import java.util.ArrayList;
import java.util.List;

public class VCOutputFile extends OutputModelObject {

    private int assertiveBatchCt;

    /**
     * All completed {@link AssertiveBlock} objects; where each
     * represents a vc or group of vcs that must be satisfied to verify a parsed
     * program.
     */
    @ModelElement
    public List<AssertiveBlock> chunks = new ArrayList<>();

    /**
     * The final list of immutable vcs.
     */
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
     * Each {@code AssertiveBlock} contains a set of VCs that refer to
     * the same set of free variables.  This method adds each {@code VC} to the
     * final list.
     *
     * @param batch         the set of {@code VC}s in context.
     * @param sectionNumber The batch number so that we can mirror the numbering
     *                      used by the Verifier. (Ideally, we should eventually
     *                      embed the name of each {@code VC} from the Verifier
     *                      with its name for greater robustness.)
     */
    private void addVCsInContext(final AssertiveBlock batch,
                                 final int sectionNumber) {
        List<PExp> vcs = batch.getFinalConfirm().getConfirmExp().splitIntoSequents();
        //System.out.println("FINAL CONF: " + batch.getFinalConfirm().getConfirmExp());
        int vcIndex = 1;
        for (PExp vc : vcs) {
            List<? extends PExp> args = vc.getSubExpressions();
            if (!(vc instanceof PApply)) continue;
            //args.get(0) would be the function name portion of the PApply;
            //so we actually do args.get(1) to get the first arg (lhs)
            VC curVC = new VC(sectionNumber + "_" + vcIndex, args.get(1), args.get(2));
            if (args.get(1).isObviouslyTrue() || args.get(2).isObviouslyTrue()) continue;
            finalVcs.add(curVC);
            vcIndex++;
        }
    }


}
