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
    private final Map<PSymbol, PSymbol> scratchwork = new HashMap<>();
    private final PExp startingExp;

    public FlexibleNameSubstitutingListener(PExp startingExp,
                                            Map<PExp, PExp> substitutions) {
        this.substitutions = substitutions;
        this.startingExp = startingExp;

        for (Map.Entry<PExp, PExp> e : substitutions.entrySet()) {
            if (e.getKey() instanceof PSymbol &&
                    e.getValue() instanceof PSymbol) {
                scratchwork.put((PSymbol)e.getKey(), (PSymbol)e.getValue());
            }
        }
    }

    @Override public void beginPSymbol(PSymbol e) {
        if ( e.isFunctionApplication() ) {
            PSymbol firstClassRefExp =
                    new PSymbol.PSymbolBuilder(e).deleteArguments().build();
            if (scratchwork.containsKey(firstClassRefExp) &&
                    !scratchwork.get(firstClassRefExp)
                            .isFunctionApplication()) {
                String newName = scratchwork.get(firstClassRefExp).getName();
                PSymbol nameModifiedExp =
                        new PSymbol.PSymbolBuilder(e).name(newName).build();
                substitutions.put(e, nameModifiedExp);
            }
        }
    }
}
