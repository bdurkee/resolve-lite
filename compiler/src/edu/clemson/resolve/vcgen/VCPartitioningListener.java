package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpVisitor;
import edu.clemson.resolve.proving.absyn.PLambda;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.rsrg.semantics.TypeGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VCPartitioningListener extends PExpVisitor {

    public final List<PExp> result = new ArrayList<>();
    PExp current;
    public List<PExp> currentAssumptions = new ArrayList<>();
    public boolean seenImplies = false;
    //anywhere you see an implies in a subtree, take the right child
    @Override public void endPSymbol(PSymbol e) {
        TypeGraph g = e.getMathType().getTypeGraph();
        if (e.getName().equals("and")) {
            if (seenImplies) {
                PExp a = g.formConjuncts( currentAssumptions.subList(0,
                        currentAssumptions.size() - (currentAssumptions.size() - 1)));
                PExp b = currentAssumptions.get(currentAssumptions.size() - 1);
                PExp c = e.getArguments().get(1);
                result.add(g.formConjunct(a, g.formImplies(b, c)));
                currentAssumptions.clear();
            }
            else {
                currentAssumptions.add(e);
            }
        }
        else if (e.getName().equals("implies")) {
            seenImplies = true;
            currentAssumptions.addAll(e.getArguments().stream()
                    .filter(arg -> !arg.containsName("implies"))
                    .collect(Collectors.toList()));
        }
    }


}
