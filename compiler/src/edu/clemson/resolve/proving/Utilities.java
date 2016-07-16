package edu.clemson.resolve.proving;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.vcgen.VC;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Utilities {

    @NotNull
    public static PExp liftLambdas(@NotNull DumbMathClssftnHandler g, @NotNull VC e) {
        ArrayList<PExp> newConjuncts = new ArrayList<>();
        java.util.List<PExp> a_p = e.getAntecedent().splitIntoConjuncts();
        for (PExp p : a_p) {
            newConjuncts.add(recursiveLift(p));
        }
        myAntecedent = new Antecedent(newConjuncts);

        newConjuncts.clear();
        a_p = myConsequent.getMutableCopy();
        for (PExp p : a_p) {
            newConjuncts.add(recursiveLift(p));
        }
        myConsequent = new Consequent(newConjuncts);

        for (PLambda p : m_liftedLamdas.keySet()) {
            String name = m_liftedLamdas.get(p);
            PExp body = p.getBody();
            PSymbol lhs =
                    new PSymbol(p.getType(), p.getTypeValue(), name, p
                            .getParameters());
            ArrayList<PExp> args = new ArrayList<PExp>();
            args.add(lhs);
            args.add(body);
            m_liftedLambdaPredicates.add(new PSymbol(m_typegraph.BOOLEAN, null,
                    "=", args));
        }
    }

}
