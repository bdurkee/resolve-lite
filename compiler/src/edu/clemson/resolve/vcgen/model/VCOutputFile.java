package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.codegen.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VC;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VCOutputFile extends OutputModelObject {

    private int currentVcNumber;
    private final RESOLVECompiler compiler;

    /**
     * All raw {@link AssertiveBlock} objects arising in this file; where each represents a vc or group of vcs that
     * must be satisfied to verify the program under consideration.
     */
    @ModelElement
    public List<AssertiveBlock> chunks = new ArrayList<>();

    /** The final list of immutable vcs. */
    @ModelElement
    public List<VC> finalVcs = new ArrayList<>();

    public VCOutputFile(@NotNull RESOLVECompiler rc) {
        this.currentVcNumber = 1;
        this.compiler = rc;
    }

    public List<VC> getFinalVCs() {
        return this.finalVcs;
    }

    public void addAssertiveBlock(AssertiveBlock b) {
        chunks.add(b);
        addVCsInContext(b);
    }

    /**
     * A convenience method for tools looking to annotate lines by {@link VC} information
     *
     * @return A mapping from line number to the VCs arising from that particular line.
     */
    public Map<Integer, List<VC>> getVCsGroupedByLineNumber() {
        Map<Integer, List<VC>> result = new LinkedHashMap<>();
        for (VC vc : finalVcs) {
            int line = vc.getLocation().getLine();
            result.putIfAbsent(line, new ArrayList<>());
            result.get(line).add(vc);
        }
        return result;
    }

    private void addVCsInContext(final AssertiveBlock batch) {

        VCConfirm batchedConfirm = batch.getFinalConfirm();
        List<PExp> sequentComponents = batchedConfirm.getConfirmExp().split();
        //System.out.println("FINAL CONF: " + batch.getFinalConfirm().getConfirmExp());

        PriorityQueue<VC> vcTempBatchOrderedByLine = new PriorityQueue<>(new Comparator<VC>() {
            @Override
            public int compare(VC o1, VC o2) {
                return o1.getNumber() <= o2.getNumber() ? -1 : 1;
            }
        });
        for (PExp e : sequentComponents) {
            List<? extends PExp> args = e.getSubExpressions();
            if (!(e instanceof PApply)) continue;   //TODO: Why is this here again?

            PExp antecedentExp = args.get(1);
            PExp consequentExp = args.get(2);
            if (args.get(2).isObviouslyTrue()) continue;

            if (consequentExp.getVCLocation() == null) {
                compiler.errMgr.toolError(ErrorKind.VC_MISSING_LOCATION_INFO, consequentExp.toString());
                continue;
            }
            VC curVC = new VC(consequentExp.getVCLocation().getLine(), antecedentExp, consequentExp);
            vcTempBatchOrderedByLine.add(curVC);
        }
        VC vc = null;
        while ((vc = vcTempBatchOrderedByLine.poll()) != null) {
            finalVcs.add(new VC(currentVcNumber, vc.getAntecedent(), vc.getConsequent()));
            currentVcNumber++;
        }
    }
}
