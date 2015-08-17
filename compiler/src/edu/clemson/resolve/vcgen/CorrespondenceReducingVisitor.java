package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpVisitor;
import edu.clemson.resolve.proving.absyn.PLambda;
import edu.clemson.resolve.proving.absyn.PSymbol;

import java.util.HashMap;
import java.util.Map;

/** A visitor that replaces conceptual variables with their correspondence
 *  defined mathematical counterparts.
 */
public class CorrespondenceReducingVisitor extends PExpVisitor {

    //"conc.P.Trmnl_Loc" -> SS(k)(P.Length, Cen(k))
    //"conc.P.Curr_Loc" -> SS(k)(P.Curr_Place, Cen(k))
    //"conc.P.Lab" -> \ q : Sp_Loc(k).({P.labl.Valu(SCD(q)) if SCD(q) + 1 <= P.Length; ...});
    private final Map<String, PExp> substitutions = new HashMap<>();
    private PExp startingExp;

    public CorrespondenceReducingVisitor(PExp correspondenceExp, PExp start) {
        for (PExp e : correspondenceExp.splitIntoConjuncts()) {
            PSymbol left = (PSymbol)e.getSubExpressions().get(0);
            substitutions.put(left.getName(), e.getSubExpressions().get(1));
        }
        BasicBetaReducingVisitor r = betaReduce(substitutions, start);
        this.substitutions.putAll(r.substitutions);
        this.startingExp = r.betaReducedExp;
    }

    public CorrespondenceReducingVisitor(Map<String, PExp> substitutions,
                                         PExp start) {
        BasicBetaReducingVisitor r = betaReduce(substitutions, start);
        this.substitutions.putAll(substitutions);
        this.startingExp = r.betaReducedExp;
    }

    private BasicBetaReducingVisitor betaReduce(Map<String, PExp> substitutions,
                                                PExp startingExp) {
        BasicBetaReducingVisitor r =
                new BasicBetaReducingVisitor(substitutions, startingExp);
        startingExp.accept(r);
        return r;
    }

    public PExp getReducedExp() {
        return startingExp;
    }

    @Override public void endPSymbol(PSymbol e) {
        if (substitutions.containsKey(e.getName())) {
            startingExp = startingExp
                    .substitute(e, substitutions.get(e.getName()));
        }
    }

    /** Doesn't handle the many of intricacies involved in performing a full,
     *  'canonical' beta-reduction such as alpha renaming, etc. Hence the
     *  'basic' in the name.
     */
    private static class BasicBetaReducingVisitor extends PExpVisitor {

        public final Map<String, PExp> substitutions;
        public PExp betaReducedExp;

        public BasicBetaReducingVisitor(Map<String, PExp> substitutions,
                                        PExp startingExp) {
            this.substitutions = substitutions;
            this.betaReducedExp = startingExp;
        }

        @Override public void endPSymbol(PSymbol e) {
            if (e.isFunctionApplication() && e.getName().startsWith("conc.")) {
                if (!(substitutions.get(e.getName()) instanceof PLambda)) {
                    throw new UnsupportedOperationException("can't b-reduce " +
                            "non-lambda function applications...");
                }
                PLambda l = (PLambda) substitutions.get(e.getName());
                PExp newBody = l.getBody().substitute(l.getParametersAsPExps(),
                        e.getArguments());
                betaReducedExp = betaReducedExp.substitute(e, newBody);
            }
        }
    }
}
