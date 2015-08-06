package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.proving.Antecedent;
import edu.clemson.resolve.proving.Consequent;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VC;
import org.rsrg.semantics.TypeGraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VCOutputFile extends OutputModelObject {

    private int assertiveBatchCt;

    /**
     * All completed {@link AssertiveBlock} objects; where each
     * represents a vc or group of vcs that must be satisfied to verify a parsed
     * program.
     */
    @ModelElement public List<AssertiveBlock> chunks = new ArrayList<>();

    /**
     * The final list of immutable vcs.
     */
    @ModelElement public List<VC> finalVcs = new ArrayList<>();

    public VCOutputFile() {
        assertiveBatchCt = 0;
    }

    public List<VC> getProverOutput() {
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
     * @param batch the set of {@code VC}s in context.
     * @param sectionNumber The batch number so that we can mirror the numbering
     *                      used by the Verifier. (Ideally, we should eventually
     *                      embed the name of each {@code VC} from the Verifier
     *                      with its name for greater robustness.)
     */
    private void addVCsInContext(final AssertiveBlock batch,
                                 final int sectionNumber) {
        PExp topLevelImplication = batch.getFinalConfirm().getConfirmExp();
        List<PExp> antecedentConjuncts = topLevelImplication.getSubExpressions()
                .get(0).splitIntoConjuncts();
        List<PExp> consequentConjuncts = topLevelImplication.getSubExpressions()
                .get(1).splitIntoConjuncts();
        //Todo: Once we write our split method figured out, let's test it on the following chunk of assertive code:
        //(((((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (Temp = Empty_String implies      S = (Reverse(Temp) o S)))) and  ((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (S = (Reverse(Temp') o S'') implies  ((1 <= |S''|) implies  (1 <= |S''|)))))) and  ((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (S = (Reverse(Temp') o S'') implies  ((1 <= |S''|) implies  ((1 + |Temp'|) <= Max_Depth)))))) and  ((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (S = (Reverse(Temp') o S'') implies  ((1 <= |S''|) implies  (S'' = (<Next_Entry'> o S') implies      S = (Reverse((<Next_Entry'> o Temp')) o S')))))))

        //idea: 1. List conjuncts = targetExp.splitIntoConjuncts()
        // 2. for each seg : conjuncts, if seg contains 'implies', .. else, coalesce into the next segment regardless

        //which should produce the following list of length 4:
        //1. ((((1 <= Max_Depth) and (|S| <= Max_Depth)) and Temp = Empty_String) implies      S = (Reverse(Temp) o S))
        //2. (((((1 <= Max_Depth) and (|S| <= Max_Depth)) and S = (Reverse(Temp') o S'')) and (1 <= |S''|)) implies  (1 <= |S''|))
        //3. (((((1 <= Max_Depth) and (|S| <= Max_Depth)) and S = (Reverse(Temp') o S'')) and (1 <= |S''|)) implies  ((1 + |Temp'|) <= Max_Depth))
        //4. ((((((1 <= Max_Depth) and (|S| <= Max_Depth)) and S = (Reverse(Temp') o S'')) and (1 <= |S''|)) and S'' = (<Next_Entry'> o S')) implies      S = (Reverse((<Next_Entry'> o Temp')) o S'))

        //partitionVCs()
        int vcIndex = 1;
        for (PExp consequent : consequentConjuncts) {
            VC curVC = new VC(sectionNumber + "_" + vcIndex,
                    new Antecedent(antecedentConjuncts),
                    new Consequent(consequent));
            finalVcs.add(curVC);
            vcIndex++;
        }
    }


}
