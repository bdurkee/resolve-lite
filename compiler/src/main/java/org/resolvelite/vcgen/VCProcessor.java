package org.resolvelite.vcgen;

import org.resolvelite.misc.Utils;
import org.resolvelite.vcgen.model.AssertiveCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class VCProcessor implements Iterable<VC> {

    /**
     * The final list of {@link VC}s from which to spawn Iterators as requested.
     * Inv: {@code finalVCs != null}.
     */
    private final List<VC> finalVCs = new ArrayList<>();

    public VCProcessor(Collection<AssertiveCode> source) {
        int sectionNumber = 0;

        //VCs come in batches that all refer to the same cluster of variables.
        //Cycle through each batch.
        for (AssertiveCode batch : source) {
            addVCsInContext(batch, sectionNumber);
            sectionNumber++;
        }
    }

    /**
     * Each {@link AssertiveCode} contains a set of vcs that refer to
     * the same set of free variables. This method adds each vc to the final
     * list.
     * 
     * @param batch The set of VCs in context.
     * @param sectionNumber The batch number so that we can mirror the numbering
     *        used by the Verifier. (Ideally, we should eventually
     *        embed the name of each VC from the Verifier with its
     *        name for greater robustness.)
     */
    private void addVCsInContext(final AssertiveCode batch,
            final int sectionNumber) {

        /*List<InfixExp> vCs = batch.getFinalConfirm().getEnclosedExp().split();
        int vcIndex = 1;

        for (InfixExp vC : vCs) {
            finalVCs.add(new VC(vC.getLeft(), vC.getRight(),
                    sectionNumber + "_" + vcIndex));
            vcIndex++;
        }*/
    }

    public Iterator<VC> iterator() {
        return finalVCs.iterator();
    }
}
