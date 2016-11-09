package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.codegen.ModelElement;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VCOutputFile {

    private int currentVcNumber;
    private final RESOLVECompiler compiler;

    /**
     * All raw {@link VCAssertiveBlock} objects arising in this file; where each represents a vc or group of vcs that
     * must be satisfied to verify the program under consideration.
     */
    public List<VCAssertiveBlock> chunks = new ArrayList<>();

    /** The final list of immutable vcs. */
    public List<VC> finalVcs = new ArrayList<>();
    public List<VC2> finalVcs2 = new ArrayList<>();

    public VCOutputFile(@NotNull RESOLVECompiler rc) {
        this.currentVcNumber = 1;
        this.compiler = rc;
    }

    public List<VC> getFinalVCs() {
        return this.finalVcs;
    }

    public void addAssertiveBlocks(List<VCAssertiveBlock> blocks) {
        for (VCAssertiveBlock block : blocks) {
            chunks.add(block);
            addVCsInContext(block);
        }
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

    private void addVCsInContext(final VCAssertiveBlock batch) {
        Set<Sequent> sequents = batch.getFinalConfirm().getSequents();

        PriorityQueue<VC2> vcTempBatchOrderedByLine = new PriorityQueue<>(new Comparator<VC2>() {
            @Override
            public int compare(VC2 o1, VC2 o2) {
                return o1.getLocation().getLine() <= o2.getLocation().getLine() ? -1 : 1;
            }
        });
        for (Sequent sequent : sequents) {
            for (PExp succeedent : sequent.getRightFormulas()) {
                if (succeedent == null) continue;
                if (succeedent.getVCLocation() == null) {
                    compiler.errMgr.toolError(ErrorKind.VC_MISSING_LOCATION_INFO, succeedent.toString());
                    continue;
                }
                VC2 vc = new VC2(succeedent.getVCLocation(), -1, succeedent.getVCExplanation(), sequent);
                vcTempBatchOrderedByLine.add(vc);
            }
        }
        VC2 vc = null;
        while ((vc = vcTempBatchOrderedByLine.poll()) != null) {
            if (vc.isObviouslyTrue()) continue;
            finalVcs2.add(new VC2(vc.getLocation(), currentVcNumber, vc.getExplanation(), vc.getSequent()));
            currentVcNumber++;
        }
    }

    @Override
    public String toString() {
        String result = "";

        for (VC2 vc : finalVcs2) {
            result += vc.toString() + "\n\n";
        }
        for (VCAssertiveBlock b : chunks) {
            result += b.getDescription() + "\n";
            result += b.getText() + "\n";
            result += "<S T E P S>\n";

            for (RuleApplicationStep step : b.getApplicationSteps()) {
                result += step + "\n\n";
            }
        }
        return result;
    }
}
