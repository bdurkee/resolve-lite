package edu.clemson.resolve.proving;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.semantics.BindingException;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author hamptos
 */
public final class PerVCProverModel {

    public static enum ChangeEventMode {

        ALWAYS {

            @Override
            public boolean report(boolean important) {
                return true;
            }
        },
        INTERMITTENT {

            private int eventCount;

            @Override
            public boolean report(boolean important) {
                eventCount++;

                return (eventCount % 300 == 0);
            }
        };

        public abstract boolean report(boolean important);
    };

    private final DumbMathClssftnHandler myTypeGraph;
    /**
     * <p>A friendly name of what we're trying to prove. Should go well with
     * "Proving XXX" and "Proof for XXX".</p>
     */
    private final String myTheoremName;
    /**
     * <p>A hashmap of local theorems for quick searching. Its keyset is always
     * the same as the set of
     * <code>PExp</code>s embedded in the
     * <code>LocalTheorem</code>s of
     * <code>myLocalTheoremsList</code>. Note that this means no
     * <code>PExp</code> in this set will have a top-level "and".</p>
     *
     * <p>Each entry in the map maps to an integer count of the number of local
     * theorems that embed that PExp, making this a representation of a
     * multiset. As an invariant, no entry will map to 0 or less.</p>
     */
    private final Map<PExp, Integer> myLocalTheoremsSet =
            new HashMap<PExp, Integer>();
    private final Set<PExp> myLocalTheoremSetForReturning;

    private int myLocalTheoremsHash;
    private int myConsequentsHash;

    /**
     * <p>A list of listeners to be contacted when the model changes. Note that
     * the behavior of change listening is modified by
     * <code>myChangeEventMode</code>.</p>
     */
    private List<ChangeListener> myChangeListeners =
            new LinkedList<ChangeListener>();

    private ChangeEventMode myChangeEventMode = ChangeEventMode.INTERMITTENT;

    public PerVCProverModel(DumbMathClssftnHandler g, String proofFor,
                            List<PExp> antecedents, List<PExp> consequents) {
        myTheoremName = proofFor;
        myLocalTheoremSetForReturning = myLocalTheoremsSet.keySet();
        myTypeGraph = g;
    }

    public String getTheoremName() {
        return myTheoremName;
    }

    public void setChangeEventMode(ChangeEventMode m) {
        myChangeEventMode = m;
    }

    public void addChangeListener(ChangeListener l) {
        myChangeListeners.add(l);
    }
}