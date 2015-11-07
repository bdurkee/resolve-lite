package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wraps a standard call to {@link PExp#substitute(Map)} with special logic
 * that preprocesses the underlying expression first, before substituting.
 * <p>
 * The preprocessing step takes any function application appearing in the
 * underlying exp, {@code startingExp}, and if it's name matches one of the keys
 * in {@code substitutions}, we perform some exp surgery on the application to
 * make the names mesh. For example, if {@code startingExp} is:
 * <pre>
 *     \@A(i) + S.Contents(j)
 * </pre> and our {@code substitutions} are:
 * <pre>
 *     \@A -> S.Contents
 *     S.Contents -> 
 * </pre>
 */
public class FlexibleNameSubstitutingListener extends PExpListener {

    private final Map<PExp, PExp> substitutions;
    private final Map<PExp, PExp> applicationSubstitutions = new HashMap<>();
    private final PExp startingExp;

    public FlexibleNameSubstitutingListener(PExp startingExp,
                                            List<PExp> existing,
                                            List<PExp> replacements) {
        if (existing.size() != replacements.size()) {
            throw new IllegalArgumentException("existing and replacement " +
                    "substitution lists must be the same size");
        }
        this.substitutions = new HashMap<>();
        Iterator<PExp> existingIter = existing.iterator();
        Iterator<PExp> replacementsIter = replacements.iterator();
        while (existingIter.hasNext()) {
            substitutions.put(existingIter.next(), replacementsIter.next());
        }
        this.startingExp = startingExp;
    }

    public FlexibleNameSubstitutingListener(PExp startingExp,
                                            Map<PExp, PExp> substitutions) {
        this.substitutions = substitutions;
        this.startingExp = startingExp;
    }

    public PExp getSubstitutedExp() {
        PExp intermediateSubstitutedExp =
                startingExp.substitute(applicationSubstitutions);
        return intermediateSubstitutedExp.substitute(substitutions);
    }

    @Override public void beginPSymbol(@NotNull PSymbol e) {
        /*if ( e.isFunctionApplication() ) {
            PExp withoutArgs = e.withArgumentsErased();
            if (substitutions.containsKey(withoutArgs) &&
                    !(substitutions.get(withoutArgs) instanceof PLambda) &&
                    !substitutions.get(withoutArgs).isFunctionApplication()) {
                PSymbol replacement = (PSymbol)substitutions.get(withoutArgs);
                applicationSubstitutions.put(e, new PSymbolBuilder(replacement)
                        .arguments(e.getArguments()).build());
            }
        }*/
    }
}
