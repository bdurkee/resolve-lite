package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpVisitor;
import edu.clemson.resolve.proving.absyn.PLambda;
import edu.clemson.resolve.proving.absyn.PSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VCPartitioningListener extends PExpVisitor {

    public final List<PExp> result = new ArrayList<>();
    private final PExp originalExp;

    public VCPartitioningListener(PExp originalExp) {
        this.originalExp = originalExp;
        if ( !(originalExp.containsName("implies") ||
                originalExp.containsName("and")) ) {
            result.add(originalExp);
        }
    }

    @Override public void beginPSymbol(PSymbol e) {
        //we don't want the actual 'implies' or 'and' exps, just
        //their subordinate terms
        if ( e.getName().equals("implies") || e.getName().equals("and") ) {
            result.addAll(e.getArguments().stream()
                    .filter(s -> !(s.containsName("and") && s.containsName("implies")))
                    .collect(Collectors.toList()));
        }
    }

    @Override public void beginPLambda(PLambda e) {
            result.add(e);
    }


}
