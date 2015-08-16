package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpVisitor;
import edu.clemson.resolve.proving.absyn.PLambda;
import edu.clemson.resolve.proving.absyn.PSymbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A visitor that replaces conceptual variables with their correspondence
 * defined mathematical equivalents.
 * <p>
 * Given a starting expression {@code startingExp}, and a mapping of
 * {@code conc} variables to their mathematical equivalents,
 * </p>
 */
public class CorrespondenceSubstitutingVisitor extends PExpVisitor {

    //"conc.P.Trmnl_Loc" -> SS(k)(P.Length, Cen(k))
    //"conc.P.Curr_Loc" -> SS(k)(P.Curr_Place, Cen(k))
    //"conc.P.Lab" -> \ q : Sp_Loc(k).({P.labl.Valu(SCD(q)) if SCD(q) + 1 <= P.Length; ...});
    private final Map<String, PExp> substitutions;
    private final PExp startingExp;

    public CorrespondenceSubstitutingVisitor(Map<String, PExp> substitutions, PExp startingExp) {
        this.substitutions = substitutions;
        this.startingExp = startingExp;
    }

    public PExp getSubstitutedExp() {
        return startingExp;
    }

    //we're walking whatever the starting expr is, for our though purpose,
    //think constraint clause of Bdd_Spiral_Template:

    //SCD(k)(conc.P.Trmnl_Loc) <= Max_Length and
    //conc.P.Curr_Loc is_in Inward_Loc(conc.P.Trmnl_Loc) and
    //conc.P.Lab(conc.P.Trmnl) = Label.Base_Point;

    @Override public void endPSymbol(PSymbol e) {
        if (e.isFunctionApplication() && e.getName().startsWith("conc.")) {
            //get the body of the function
            PExp g = substitutions.get(e.getName());
            List<PExp> actuals = e.getArguments();
            List<PExp> formals = new ArrayList<>();
            if (g instanceof PLambda) {
                formals.addAll(((PLambda) g).getParameters());
            }
            int i;
            i = 0;
        }
        else {
            if (substitutions.containsKey(e.getName())) {
                startingExp.substitute(e, substitutions.get(e.getName()));
            }
        }
    }
}
