package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a vanilla application of {@link PExp#substitute(Map)} with special
 * logic that updates/replaces key value pairs in the provided
 * {@code substitutions} map with information
 */
public class FlexibleNameSubstitutingListener extends PExpListener {

    private final Map<PExp, PExp> substitutions;
    private final PExp startingExp;

    public FlexibleNameSubstitutingListener(PExp startingExp,
                                            Map<PExp, PExp> substitutions) {
        this.substitutions = substitutions;
        this.startingExp = startingExp;
    }

    @Override public void beginPSymbol(PSymbol e) {
        if ( e.isFunctionApplication() ) {

            if (substitutions.containsKey(e.withArgumentsErased()) &&
                    !substitutions.get(e.withArgumentsErased())
                            .isFunctionApplication()) {
                String newName = scratchwork.get(firstClassRefExp).getName();
                PSymbol nameModifiedExp =
                        new PSymbol.PSymbolBuilder(e).name(newName).build();
                substitutions.put(e, nameModifiedExp);
            }
        }
    }
}
