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
    boolean seenAndAfterImplies;
    //anywhere you see an implies in a subtree, take the right child
    @Override public void beginPSymbol(PSymbol e) {
        if (e.getName().equals("implies") &&
                e.getArguments().get(1).containsName("and")) {
            List<PExp> stub = e.getArguments().get(1).splitIntoConjuncts();
            System.out.println("SPLIT: " + e.getArguments().get(0) + " IMPLIES " + stub.get(0));
        }
    }


}
